package com.nhom08.qlychitieu.tien_ich;

import android.content.Context;
import android.content.SharedPreferences;

import com.nhom08.qlychitieu.mo_hinh.User;
public class SharedPrefUtils {
    private static final String PREF_NAME = "UserSession";

    public static void saveUser(Context context, User user) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        if (user != null) {
            editor.putInt("user_id", user.getUserId());
            editor.putString("email", user.getEmail());
            editor.putString("avatar_path", user.getAvatarPath());
            editor.apply();
        }
    }

    public static User getUser(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        int userId = prefs.getInt("user_id", -1);

        if (userId == -1) return null;

        String email = prefs.getString("email", "");
        String avatarPath = prefs.getString("avatar_path", "");

        User user = new User();
        user.setUserId(userId);
        user.setEmail(email);
        user.setAvatarPath(avatarPath);

        return user;
    }

    public static void clearUser(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().clear().apply();
    }
}
