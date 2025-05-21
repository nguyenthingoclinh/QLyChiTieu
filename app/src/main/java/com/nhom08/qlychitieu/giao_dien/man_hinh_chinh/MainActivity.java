package com.nhom08.qlychitieu.giao_dien.man_hinh_chinh;

import static com.nhom08.qlychitieu.tien_ich.Constants.REQUEST_NOTIFICATION_PERMISSION_CODE;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.SparseArray;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.nhom08.qlychitieu.R;
import com.nhom08.qlychitieu.databinding.ActivityMainBinding;
import com.nhom08.qlychitieu.giao_dien.giao_dich.AddTransactionActivity;
import com.nhom08.qlychitieu.tien_ich.ThemeUtils;
import com.nhom08.qlychitieu.tien_ich.MessageUtils;
import java.util.function.Supplier;

public class MainActivity extends AppCompatActivity {

    ActivityMainBinding binding;
    private MessageUtils messageUtils;
    private final SparseArray<Supplier<Fragment>> fragmentMap = new SparseArray<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeUtils.applyTheme(this);
        // Khởi tạo channel thông báo
        NotificationHelper.createNotificationChannel(this);
        EdgeToEdge.enable(this);
        messageUtils = new MessageUtils(this);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Xử lý insets cho system bars
        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, windowInsets) -> {
            WindowInsetsCompat.Type.systemBars();
            return WindowInsetsCompat.CONSUMED;
        });


        // Tính toán và áp dụng padding cho FrameLayout dựa trên chiều cao của BottomNavigationView
        binding.bottomNavigationView.post(() -> {
            int bottomNavHeight = binding.bottomNavigationView.getHeight();
            binding.frameLayout.setPadding(0, 0, 0, bottomNavHeight);
        });

        // Khởi tạo ánh xạ ID với Fragment
        fragmentMap.put(R.id.menu_home, HomeFragment::new);
        fragmentMap.put(R.id.menu_char, ChartFragment::new);
        fragmentMap.put(R.id.menu_report, ReportFragment::new);
        fragmentMap.put(R.id.menu_person, PersonFragment::new);

        // Thiết lập BottomNavigationView
        binding.bottomNavigationView.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.menu_add) {
                startActivity(new Intent(this, AddTransactionActivity.class));
                return false; // không chọn tab này
            }

            Supplier<Fragment> fragmentSupplier = fragmentMap.get(item.getItemId());
            if (fragmentSupplier != null) {
                replaceFragment(fragmentSupplier.get());
            }
            return true;
        });

        // Mặc định hiển thị HomeFragment khi khởi động
        replaceF();

        // Lắng nghe khi back stack thay đổi để cập nhật bottom nav
        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            Fragment current = getSupportFragmentManager().findFragmentById(R.id.frame_layout);
            if (current instanceof HomeFragment) {
                binding.bottomNavigationView.setSelectedItemId(R.id.menu_home);
            } else if (current instanceof ChartFragment) {
                binding.bottomNavigationView.setSelectedItemId(R.id.menu_char);
            } else if (current instanceof ReportFragment) {
                binding.bottomNavigationView.setSelectedItemId(R.id.menu_report);
            } else if (current instanceof PersonFragment) {
                binding.bottomNavigationView.setSelectedItemId(R.id.menu_person);
            }
        });

    }

    private void replaceF() {
        replaceFragment(new HomeFragment());
    }

    private void replaceFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment currentFragment = fragmentManager.findFragmentById(R.id.frame_layout);
        if (currentFragment != null && currentFragment.getClass() == fragment.getClass()) {
            return;
        }

        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        // Thêm animation
        fragmentTransaction.setCustomAnimations(
                R.anim.enter_from_right,  // Fragment mới xuất hiện
                R.anim.exit_to_left,      // Fragment cũ biến mất
                R.anim.enter_from_left,   // Fragment trước xuất hiện khi quay lại
                R.anim.exit_to_right      // Fragment hiện tại biến mất khi quay lại
        );

        fragmentTransaction.setReorderingAllowed(true);
        fragmentTransaction.replace(R.id.frame_layout, fragment);
        if(!(fragment instanceof HomeFragment)) {
            fragmentTransaction.addToBackStack(null);
        }
        fragmentTransaction.commit();
    }

    /**
     * Kiểm tra quyền thông báo và yêu cầu nếu cần
     */
    private void checkNotificationPermission() {
        // Chỉ cần kiểm tra quyền trên Android 13 trở lên
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                // Hiển thị giải thích nếu cần
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.POST_NOTIFICATIONS)) {
                    // Hiển thị dialog giải thích
                    showNotificationPermissionRationale();
                } else {
                    // Trực tiếp yêu cầu quyền
                    requestNotificationPermission();
                }
            }
        }
    }

    /**
     * Hiển thị dialog giải thích tại sao cần quyền thông báo
     */
    private void showNotificationPermissionRationale() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Cần quyền thông báo")
                .setMessage("Ứng dụng cần quyền thông báo để gửi cảnh báo khi chi tiêu vượt quá ngưỡng.")
                .setPositiveButton("Đồng ý", (dialog, which) -> requestNotificationPermission())
                .setNegativeButton("Để sau", null)
                .create()
                .show();
    }

    /**
     * Yêu cầu quyền thông báo
     */
    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    REQUEST_NOTIFICATION_PERMISSION_CODE
            );
        }
    }

    /**
     * Xử lý kết quả yêu cầu quyền
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_NOTIFICATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                messageUtils.showSuccess("Quyền thông báo đã được cấp");
                // Có thể thực hiện các hành động cần quyền thông báo ở đây
            } else {
                messageUtils.showError("Bạn sẽ không nhận được thông báo chi tiêu. Bạn có thể bật quyền này trong cài đặt ứng dụng.");
            }
        }
    }
}