package com.nhom08.qlychitieu.tien_ich;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import androidx.appcompat.app.AppCompatDelegate;

public class ThemeUtils {
    public static final String THEME_PREFS = "theme_prefs";
    public static final String KEY_THEME_MODE = "theme_mode";

    // Các chế độ theme
    public static final int THEME_FOLLOW_SYSTEM = 0;  // Theo hệ thống
    public static final int THEME_LIGHT = 1;          // Luôn sáng
    public static final int THEME_DARK = 2;           // Luôn tối

    // Áp dụng theme theo cài đặt đã lưu
    public static void applyTheme(Context context) {
        int themeMode = getThemeMode(context);

        switch (themeMode) {
            case THEME_LIGHT:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case THEME_DARK:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case THEME_FOLLOW_SYSTEM:
            default:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                } else {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY);
                }
                break;
        }
    }

    // Lưu chế độ theme vào SharedPreferences
    public static void saveThemeMode(Context context, int themeMode) {
        SharedPreferences prefs = context.getSharedPreferences(THEME_PREFS, Context.MODE_PRIVATE);
        prefs.edit().putInt(KEY_THEME_MODE, themeMode).apply();
        applyTheme(context);
    }

    // Lấy chế độ theme từ SharedPreferences
    public static int getThemeMode(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(THEME_PREFS, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_THEME_MODE, THEME_FOLLOW_SYSTEM);
    }
}