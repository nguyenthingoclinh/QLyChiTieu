package com.nhom08.qlychitieu.giao_dien.thong_bao;

import android.app.AlarmManager;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.nhom08.qlychitieu.MyApplication;
import com.nhom08.qlychitieu.R;
import com.nhom08.qlychitieu.adapter.AlertAdapter;
import com.nhom08.qlychitieu.csdl.AppDatabase;
import com.nhom08.qlychitieu.databinding.ActivityNotificationSettingsBinding;
import com.nhom08.qlychitieu.mo_hinh.SpendingAlert;
import com.nhom08.qlychitieu.tien_ich.Constants;
import com.nhom08.qlychitieu.tien_ich.NotificationHelper;

import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * Activity để quản lý cài đặt thông báo và cảnh báo chi tiêu
 */
public class NotificationSettingsActivity extends AppCompatActivity {

    private static final String TAG = "NotificationSettings";
    // Thêm hằng số
    private static final int REQUEST_SCHEDULE_EXACT_ALARM = 1001;
    private ActivityNotificationSettingsBinding binding;
    private SharedPreferences prefs;
    private AlertAdapter alertAdapter;
    private AppDatabase database;
    private ExecutorService executorService;
    private int currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityNotificationSettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Lấy thực thể của application
        MyApplication myApp = (MyApplication) getApplication();

        // Khởi tạo các thành phần
        database = myApp.getDatabase();
        executorService = myApp.getExecutorService();

        // Lấy user ID
        currentUserId = myApp.getCurrentUserId();
        prefs = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);

        // Back button
        binding.tvBack.setOnClickListener(v -> finish());

        // Thiết lập các thành phần UI
        setupNotificationToggle();
        setupTimeSelector();
        setupAlertsList();
    }

    /**
     * Thiết lập cho công tắc bật/tắt thông báo
     */
    private void setupNotificationToggle() {
        // Lấy trạng thái hiện tại
        boolean notificationsEnabled = prefs.getBoolean(Constants.KEY_NOTIFICATIONS_ENABLED, true);
        binding.switchEnableNotifications.setChecked(notificationsEnabled);

        // Đăng ký sự kiện thay đổi
        binding.switchEnableNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Lưu trạng thái mới
            prefs.edit().putBoolean(Constants.KEY_NOTIFICATIONS_ENABLED, isChecked).apply();

            // Cập nhật lịch thông báo
            if (isChecked) {
                NotificationHelper.scheduleDailyReminder(this);
                Toast.makeText(this, "Đã bật thông báo nhắc nhở hàng ngày", Toast.LENGTH_SHORT).show();
            } else {
                NotificationHelper.cancelDailyReminder(this);
                Toast.makeText(this, "Đã tắt thông báo nhắc nhở hàng ngày", Toast.LENGTH_SHORT).show();
            }
        });

        // Thêm nút kiểm tra thông báo
        binding.btnTestNotification.setOnClickListener(v -> {
            if (binding.switchEnableNotifications.isChecked()) {
                // Hiển thị thông báo ngay lập tức để kiểm tra
                NotificationHelper.showDailyReminderNotification(this);
                Toast.makeText(this, "Đã hiển thị thông báo thử nghiệm", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Vui lòng bật thông báo trước", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Thiết lập cho bộ chọn thời gian
     */
    private void setupTimeSelector() {
        // Lấy thời gian hiện tại từ SharedPreferences
        String timeString = prefs.getString(Constants.KEY_NOTIFICATION_TIME, Constants.DEFAULT_NOTIFICATION_TIME);
        binding.tvNotificationTime.setText(timeString);

        binding.tvNotificationTime.setOnClickListener(v -> {
            // Parse thời gian hiện tại
            String[] timeParts = timeString.split(":");
            int hour = Integer.parseInt(timeParts[0]);
            int minute = Integer.parseInt(timeParts[1]);

            // Hiển thị TimePickerDialog
            TimePickerDialog timePickerDialog = new TimePickerDialog(
                    this,
                    (view, hourOfDay, minute1) -> {
                        // Format và lưu thời gian mới
                        String newTime = String.format("%02d:%02d", hourOfDay, minute1);
                        prefs.edit().putString(Constants.KEY_NOTIFICATION_TIME, newTime).apply();
                        binding.tvNotificationTime.setText(newTime);

                        // Cập nhật lịch thông báo nếu đang bật
                        if (binding.switchEnableNotifications.isChecked()) {
                            NotificationHelper.scheduleDailyReminder(NotificationSettingsActivity.this);
                            Toast.makeText(
                                    NotificationSettingsActivity.this,
                                    "Thông báo sẽ hiển thị lúc " + newTime + " hàng ngày",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    },
                    hour,
                    minute,
                    true  // 24h format
            );
            timePickerDialog.show();
        });
    }

    /**
     * Thiết lập cho danh sách cảnh báo
     */
    private void setupAlertsList() {
        // Thiết lập RecyclerView
        alertAdapter = new AlertAdapter(this);
        binding.recyclerViewAlerts.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerViewAlerts.setAdapter(alertAdapter);

        // Đăng ký sự kiện cho adapter
        alertAdapter.setOnAlertActionListener(new AlertAdapter.OnAlertActionListener() {
            @Override
            public void onAlertEdit(SpendingAlert alert) {
                // Mở dialog sửa cảnh báo
                openEditAlertDialog(alert);
            }

            @Override
            public void onAlertDelete(SpendingAlert alert) {
                // Xóa cảnh báo
                deleteAlert(alert);
            }

            @Override
            public void onAlertActiveChanged(SpendingAlert alert, boolean isActive) {
                // Cập nhật trạng thái cảnh báo
                updateAlertStatus(alert, isActive);
            }
        });

        // Thiết lập sự kiện cho nút thêm cảnh báo
        binding.btnAddAlert.setOnClickListener(v -> openAddAlertDialog());

        // Tải danh sách cảnh báo
        loadAlerts();
    }

    /**
     * Mở dialog thêm cảnh báo mới
     */
    private void openAddAlertDialog() {
        AlertDialogFragment dialog = AlertDialogFragment.newInstance(currentUserId);
        dialog.setOnAlertSaveListener(this::saveNewAlert);
        dialog.show(getSupportFragmentManager(), "AddAlertDialog");
    }

    /**
     * Mở dialog sửa cảnh báo
     */
    private void openEditAlertDialog(SpendingAlert alert) {
        AlertDialogFragment dialog = AlertDialogFragment.newInstance(alert);
        dialog.setOnAlertSaveListener(this::updateAlert);
        dialog.show(getSupportFragmentManager(), "EditAlertDialog");
    }

    /**
     * Lưu cảnh báo mới
     */
    private void saveNewAlert(SpendingAlert alert) {
        executorService.execute(() -> {
            try {
                // Thêm mới vào database
                long alertId = database.spendingAlertDao().insert(alert);

                // Đặt lại ID từ database
                alert.setAlertId((int) alertId);

                // Cập nhật UI
                runOnUiThread(() -> {
                    Toast.makeText(this,
                            "Đã thêm cảnh báo mới: " + alert.getTitle(),
                            Toast.LENGTH_SHORT).show();
                    loadAlerts(); // Tải lại danh sách
                });
            } catch (Exception e) {
                Log.e(TAG, "Lỗi khi lưu cảnh báo mới", e);
                runOnUiThread(() -> {
                    Toast.makeText(this,
                            "Lỗi khi lưu: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    /**
     * Cập nhật cảnh báo hiện có
     */
    private void updateAlert(SpendingAlert alert) {
        executorService.execute(() -> {
            try {
                // Cập nhật vào database
                database.spendingAlertDao().update(alert);

                // Cập nhật UI
                runOnUiThread(() -> {
                    Toast.makeText(this,
                            "Đã cập nhật cảnh báo: " + alert.getTitle(),
                            Toast.LENGTH_SHORT).show();
                    loadAlerts(); // Tải lại danh sách
                });
            } catch (Exception e) {
                Log.e(TAG, "Lỗi khi cập nhật cảnh báo", e);
                runOnUiThread(() -> {
                    Toast.makeText(this,
                            "Lỗi khi cập nhật: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    /**
     * Xóa cảnh báo
     */
    private void deleteAlert(SpendingAlert alert) {
        // Hiển thị dialog xác nhận trước khi xóa
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Xác nhận xóa")
                .setMessage("Bạn có chắc muốn xóa cảnh báo này?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    // Thực hiện xóa cảnh báo
                    executorService.execute(() -> {
                        try {
                            // Xóa khỏi database
                            database.spendingAlertDao().delete(alert);

                            runOnUiThread(() -> {
                                Toast.makeText(this,
                                        "Đã xóa cảnh báo: " + alert.getTitle(),
                                        Toast.LENGTH_SHORT).show();
                                loadAlerts(); // Tải lại danh sách
                            });
                        } catch (Exception e) {
                            Log.e(TAG, "Lỗi khi xóa cảnh báo", e);
                            runOnUiThread(() -> {
                                Toast.makeText(this,
                                        "Lỗi khi xóa: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                            });
                        }
                    });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    /**
     * Cập nhật trạng thái cảnh báo
     */
    private void updateAlertStatus(SpendingAlert alert, boolean isActive) {
        executorService.execute(() -> {
            try {
                // Cập nhật trạng thái trong model
                alert.setActive(isActive);

                // Cập nhật vào database
                database.spendingAlertDao().updateActiveStatus(alert.getAlertId(), isActive);

                runOnUiThread(() -> {
                    String message = isActive ?
                            "Đã bật cảnh báo: " + alert.getTitle() :
                            "Đã tắt cảnh báo: " + alert.getTitle();
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                Log.e(TAG, "Lỗi khi cập nhật trạng thái cảnh báo", e);
                runOnUiThread(() -> {
                    Toast.makeText(this,
                            "Lỗi: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    // Phục hồi trạng thái cũ trong UI
                    loadAlerts();
                });
            }
        });
    }

    /**
     * Tải danh sách cảnh báo từ database
     */
    private void loadAlerts() {
        executorService.execute(() -> {
            try {
                // Lấy danh sách cảnh báo từ database
                List<SpendingAlert> alerts = database.spendingAlertDao().getByUserId(currentUserId);

                runOnUiThread(() -> {
                    if (alerts.isEmpty()) {
                        binding.emptyAlertsView.setVisibility(View.VISIBLE);
                        binding.recyclerViewAlerts.setVisibility(View.GONE);
                    } else {
                        binding.emptyAlertsView.setVisibility(View.GONE);
                        binding.recyclerViewAlerts.setVisibility(View.VISIBLE);
                        alertAdapter.setAlerts(alerts);
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Lỗi khi tải danh sách cảnh báo", e);
                runOnUiThread(() -> {
                    Toast.makeText(this,
                            "Lỗi khi tải dữ liệu: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    binding.emptyAlertsView.setVisibility(View.VISIBLE);
                    binding.recyclerViewAlerts.setVisibility(View.GONE);
                });
            }
        });
    }


    @Override
    protected void onStart() {
        super.onStart();

        // Kiểm tra quyền đặt cảnh báo chính xác cho Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (!alarmManager.canScheduleExactAlarms()) {
                // Hiển thị thông báo yêu cầu quyền
                new androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle("Cần quyền cảnh báo")
                        .setMessage("Ứng dụng cần quyền đặt cảnh báo chính xác để thông báo đúng giờ. Vui lòng cấp quyền trong cài đặt.")
                        .setPositiveButton("Cài đặt", (dialog, which) -> {
                            try {
                                // Mở màn hình cài đặt quyền
                                Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                                intent.setData(Uri.parse("package:" + getPackageName()));
                                startActivityForResult(intent, REQUEST_SCHEDULE_EXACT_ALARM);
                            } catch (Exception e) {
                                Toast.makeText(this, "Không thể mở cài đặt quyền", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setNegativeButton("Để sau", null)
                        .show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_SCHEDULE_EXACT_ALARM) {
            // Kiểm tra quyền sau khi người dùng quay lại từ cài đặt
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                if (alarmManager.canScheduleExactAlarms()) {
                    // Quyền đã được cấp, thiết lập lại thông báo
                    if (binding.switchEnableNotifications.isChecked()) {
                        NotificationHelper.scheduleDailyReminder(this);
                        Toast.makeText(this, "Đã cài đặt thông báo hàng ngày", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this, "Cần quyền này để thông báo chính xác", Toast.LENGTH_SHORT).show();
                    // Tắt switch nếu không có quyền
                    binding.switchEnableNotifications.setChecked(false);
                }
            }
        } else if (resultCode == RESULT_OK && (requestCode == Constants.REQUEST_ADD_ALERT ||
                requestCode == Constants.REQUEST_EDIT_ALERT)) {
            // Tải lại danh sách cảnh báo sau khi thêm/sửa
            loadAlerts();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}