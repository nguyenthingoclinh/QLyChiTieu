package com.nhom08.qlychitieu;

import android.app.Application;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.room.Room;
import com.nhom08.qlychitieu.csdl.AppDatabase;
import com.nhom08.qlychitieu.mo_hinh.User;
import com.nhom08.qlychitieu.tien_ich.Constants;
import com.nhom08.qlychitieu.tien_ich.FormatUtils;
import com.nhom08.qlychitieu.tien_ich.NotificationHelper;
import com.nhom08.qlychitieu.tien_ich.ThemeUtils;
import com.nhom08.qlychitieu.truy_van.UserDAO;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MyApplication extends Application {
    private static MyApplication instance;
    private AppDatabase database;
    private ExecutorService executorService;
    private SharedPreferences sharedPreferences;
    private User currentUser;

    private static final int NUMBER_OF_THREADS = 4;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        // Khởi tạo ExecutorService
        executorService = Executors.newFixedThreadPool(NUMBER_OF_THREADS);

        // Khởi tạo SharedPreferences
        sharedPreferences = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);

        // Khởi tạo channel thông báo
        NotificationHelper.createNotificationChannel(this);

        // Áp dụng theme
        ThemeUtils.applyTheme(this);

        // Khởi tạo các cài đặt định dạng mặc định
        initDefaultFormats();

        // Khởi tạo Room Database
        database = Room.databaseBuilder(this,
                        AppDatabase.class, "qly_chi_tieu_db")
                .addMigrations(AppDatabase.MIGRATION_1_2)
                .addMigrations(AppDatabase.MIGRATION_2_3)
                .build();

        // Khôi phục phiên đăng nhập
        restoreUserSession();

        // Thiết lập thông báo nhắc nhở hàng ngày nếu người dùng đã đăng nhập
        setupDailyNotification();
    }

    /**
     * Thiết lập thông báo nhắc nhở hàng ngày
     */
    private void setupDailyNotification() {
        try {
            // Kiểm tra xem người dùng đã đăng nhập chưa
            boolean isLoggedIn = sharedPreferences.getBoolean(Constants.KEY_IS_LOGGED_IN, false);
            if (isLoggedIn) {
                // Kiểm tra xem thông báo có được bật không
                boolean notificationsEnabled = sharedPreferences.getBoolean(Constants.KEY_NOTIFICATIONS_ENABLED, true);
                if (notificationsEnabled) {
                    NotificationHelper.scheduleDailyReminder(this);
                }
            }
        } catch (Exception e) {
            // Log lỗi nhưng không để crash ứng dụng
            Log.e("MyApplication", "Lỗi khi thiết lập thông báo: " + e.getMessage());
        }
    }

    /**
     * Khởi tạo các cài đặt định dạng mặc định
     */
    private void initDefaultFormats() {
        // Kiểm tra và khởi tạo định dạng tiền tệ mặc định
        SharedPreferences formatPrefs = getSharedPreferences(FormatUtils.FORMAT_PREFS, MODE_PRIVATE);

        if (!formatPrefs.contains(FormatUtils.KEY_CURRENCY_CODE)) {
            formatPrefs.edit().putString(FormatUtils.KEY_CURRENCY_CODE, "VND").apply();
        }

        if (!formatPrefs.contains(FormatUtils.KEY_NUMBER_FORMAT_STYLE)) {
            formatPrefs.edit().putInt(FormatUtils.KEY_NUMBER_FORMAT_STYLE, FormatUtils.FORMAT_STYLE_US).apply();
        }

        if (!formatPrefs.contains(FormatUtils.KEY_LOCALE_CODE)) {
            formatPrefs.edit().putString(FormatUtils.KEY_LOCALE_CODE, "vi").apply();
        }
    }

    private void restoreUserSession() {
        int userId = sharedPreferences.getInt(Constants.KEY_USER_ID, -1);
        if (userId != -1) {
            executorService.execute(() -> {
                User user = database.userDao().getUserById(userId);
                if (user != null) {
                    currentUser = user;
                }
            });
        }
    }

    public static MyApplication getInstance() {
        return instance;
    }

    public AppDatabase getDatabase() {
        return database;
    }
    public UserDAO getUserDAO() {
        return database.userDao();
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        SharedPreferences.Editor editor = sharedPreferences.edit();

        if (user != null) {
            editor.putInt(Constants.KEY_USER_ID, user.getUserId())
                    .putString(Constants.KEY_USER_EMAIL, user.getEmail())
                    .putBoolean(Constants.KEY_IS_LOGGED_IN, true);
        } else {
            editor.clear();
        }
        editor.apply();

        // Cập nhật thông báo sau khi đăng nhập/đăng xuất
        if (user != null) {
            setupDailyNotification();
        } else {
            NotificationHelper.cancelDailyReminder(this);
        }
    }

    public int getCurrentUserId() {
        return sharedPreferences.getInt(Constants.KEY_USER_ID, -1);
    }

    public void clearUserSession() {
        setCurrentUser(null);
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        if (executorService != null) {
            executorService.shutdown();
        }
    }
}