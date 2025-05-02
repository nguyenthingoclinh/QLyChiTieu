package com.nhom08.qlychitieu.giao_dien.man_hinh_chinh;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.SparseArray;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.nhom08.qlychitieu.R;
import com.nhom08.qlychitieu.databinding.ActivityMainBinding;
import com.nhom08.qlychitieu.main_fragment.CharFragment;
import com.nhom08.qlychitieu.main_fragment.HomeFragment;
import com.nhom08.qlychitieu.main_fragment.PersonFragment;
import com.nhom08.qlychitieu.main_fragment.ReportFragment;
import com.nhom08.qlychitieu.giao_dien.giao_dich.AddTransactionActivity;

import java.util.function.Supplier;

public class MainActivity extends AppCompatActivity {
    ActivityMainBinding binding;
    private final SparseArray<Supplier<Fragment>> fragmentMap = new SparseArray<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Chỉ bật EdgeToEdge trên API 29 trở lên
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            EdgeToEdge.enable(this);
        }
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Xử lý insets cho system bars
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0); // Không áp dụng padding bottom
            return insets;
        });

        // Tính toán và áp dụng padding cho FrameLayout dựa trên chiều cao của BottomNavigationView
        binding.bottomNavigationView.post(() -> {
            int bottomNavHeight = binding.bottomNavigationView.getHeight();
            binding.frameLayout.setPadding(0, 0, 0, bottomNavHeight);
        });

        // Khởi tạo ánh xạ ID với Fragment
        fragmentMap.put(R.id.menu_home, HomeFragment::new);
        fragmentMap.put(R.id.menu_char, CharFragment::new);
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
            } else if (current instanceof CharFragment) {
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
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }
}