package com.nhom08.qlychitieu.tien_ich;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * BroadcastReceiver để thiết lập lại thông báo khi thiết bị khởi động
 */
public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = BootReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() != null && intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            Log.d(TAG, "Thiết bị vừa khởi động. Thiết lập lại thông báo nhắc nhở...");

            // Kiểm tra xem thông báo có được bật không
            SharedPreferences prefs = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
            boolean notificationsEnabled = prefs.getBoolean(Constants.KEY_NOTIFICATIONS_ENABLED, true);

            if (notificationsEnabled) {
                // Tạo lại channel thông báo và lên lịch thông báo
                NotificationHelper.createNotificationChannel(context);
                NotificationHelper.scheduleDailyReminder(context);
                Log.d(TAG, "Đã thiết lập lại thông báo nhắc nhở hàng ngày");
            }
        }
    }
}