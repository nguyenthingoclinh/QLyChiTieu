package com.nhom08.qlychitieu.tien_ich;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.Date;

/**
 * BroadcastReceiver để nhận và xử lý thông báo nhắc nhở chi tiêu hàng ngày
 */
public class DailyNotificationReceiver extends BroadcastReceiver {
    private static final String TAG = DailyNotificationReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Đã nhận broadcast để hiển thị thông báo hàng ngày vào lúc: " + new Date().toString());

        try {
            // Hiển thị thông báo nhắc nhở
            NotificationHelper.showDailyReminderNotification(context);

            // Thêm log để xác nhận
            Log.d(TAG, "Đã gọi showDailyReminderNotification");

            // Lên lịch lại cho ngày hôm sau
            NotificationHelper.scheduleDailyReminder(context);

            // Thêm log để xác nhận
            Log.d(TAG, "Đã gọi scheduleDailyReminder");
        } catch (Exception e) {
            Log.e(TAG, "Lỗi trong onReceive: " + e.getMessage(), e);
        }
    }
}