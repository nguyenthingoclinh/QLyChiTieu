package com.nhom08.qlychitieu;

import android.app.Application;
import android.content.SharedPreferences;
import androidx.room.Room;
import com.nhom08.qlychitieu.csdl.AppDatabase;
import com.nhom08.qlychitieu.mo_hinh.User;
import com.nhom08.qlychitieu.tien_ich.Constants;
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

        // Khởi tạo Room Database
        database = Room.databaseBuilder(this,
                        AppDatabase.class, "qly_chi_tieu_db")
                .addMigrations(AppDatabase.MIGRATION_1_2)
                .build();

        // Khôi phục phiên đăng nhập
        restoreUserSession();
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