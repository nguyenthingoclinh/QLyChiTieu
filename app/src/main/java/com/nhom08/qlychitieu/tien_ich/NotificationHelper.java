package com.nhom08.qlychitieu.tien_ich;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.os.Build;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import android.Manifest;
import android.util.Log;

import com.nhom08.qlychitieu.R;
import com.nhom08.qlychitieu.giao_dien.man_hinh_chinh.MainActivity;

import java.util.Calendar;

/**
 * Helper class cho việc tạo và quản lý thông báo trong ứng dụng
 */
public class NotificationHelper {

    private static final String TAG = NotificationHelper.class.getSimpleName();
    private static final int DAILY_NOTIFICATION_ID = 1001;

    /**
     * Tạo channel thông báo (yêu cầu cho Android 8.0+)
     */
    public static void createNotificationChannel(Context context) {
        NotificationChannel channel = new NotificationChannel(
                Constants.NOTIFICATION_CHANNEL_ID,
                Constants.NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
        );

        channel.setDescription("Kênh thông báo nhắc nhở chi tiêu hàng ngày");
        channel.enableLights(true);
        channel.setLightColor(Color.BLUE);
        channel.enableVibration(true);
        channel.setVibrationPattern(new long[]{0, 250, 250, 250});
        channel.setShowBadge(true); // Hiển thị badge trên icon ứng dụng
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC); // Hiển thị trên màn hình khóa

        NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
        if (notificationManager != null) {
            notificationManager.createNotificationChannel(channel);
            Log.d(TAG, "Đã tạo notification channel: " + Constants.NOTIFICATION_CHANNEL_ID);
        }
    }

    /**
     * Hiển thị thông báo nhắc nhở chi tiêu hàng ngày
     */
    public static void showDailyReminderNotification(Context context) {
        // Kiểm tra quyền thông báo
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "Không có quyền hiển thị thông báo");
                return;
            }
        }

        // Intent khi người dùng nhấn vào thông báo
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        // Tạo thông báo
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notifications)
                .setContentTitle("Nhắc nhở chi tiêu hàng ngày")
                .setContentText("Đã đến giờ cập nhật chi tiêu hôm nay của bạn!")
                .setPriority(NotificationCompat.PRIORITY_HIGH) // Thay đổi từ DEFAULT -> HIGH
                .setContentIntent(pendingIntent)
                .setCategory(NotificationCompat.CATEGORY_REMINDER) // Thêm category
                .setAutoCancel(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // Hiển thị trên màn hình khóa
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)) // Thêm âm thanh
                .setVibrate(new long[]{0, 250, 250, 250}); // Thêm rung

        // Hiển thị thông báo
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        try {
            notificationManager.notify(DAILY_NOTIFICATION_ID, builder.build());
            Log.d(TAG, "Đã hiển thị thông báo nhắc nhở hàng ngày");
        } catch (SecurityException e) {
            Log.e(TAG, "Lỗi khi hiển thị thông báo: " + e.getMessage());
        }
    }

    /**
     * Lên lịch thông báo nhắc nhở hàng ngày
     */
    public static void scheduleDailyReminder(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);

        // Kiểm tra xem thông báo có được bật không
        boolean notificationsEnabled = prefs.getBoolean(Constants.KEY_NOTIFICATIONS_ENABLED, true);
        if (!notificationsEnabled) {
            Log.d(TAG, "Thông báo đã bị tắt trong cài đặt");
            cancelDailyReminder(context);
            return;
        }

        // Lấy thời gian thông báo từ SharedPreferences (mặc định là 21:00)
        String timeString = prefs.getString(Constants.KEY_NOTIFICATION_TIME, Constants.DEFAULT_NOTIFICATION_TIME);
        String[] timeParts = timeString.split(":");
        int hour = Integer.parseInt(timeParts[0]);
        int minute = Integer.parseInt(timeParts[1]);

        // Tạo Intent và PendingIntent để kích hoạt thông báo
        Intent intent = new Intent(context, DailyNotificationReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, DAILY_NOTIFICATION_ID, intent, PendingIntent.FLAG_IMMUTABLE);

        // Đặt thời gian cho thông báo
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);

        // Nếu thời gian đã qua, đặt cho ngày hôm sau
        if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        // Đặt lịch thông báo
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    // Android 12 (API 31) trở lên - Kiểm tra quyền
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(
                                AlarmManager.RTC_WAKEUP,
                                calendar.getTimeInMillis(),
                                pendingIntent
                        );
                    } else {
                        // Sử dụng alarm không chính xác nếu không có quyền
                        alarmManager.setAndAllowWhileIdle(
                                AlarmManager.RTC_WAKEUP,
                                calendar.getTimeInMillis(),
                                pendingIntent
                        );
                        Log.w(TAG, "Không có quyền đặt cảnh báo chính xác, sử dụng cảnh báo không chính xác");
                    }
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    // Android 6-11
                    alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            calendar.getTimeInMillis(),
                            pendingIntent
                    );
                } else {
                    // Các phiên bản Android cũ hơn
                    alarmManager.setExact(
                            AlarmManager.RTC_WAKEUP,
                            calendar.getTimeInMillis(),
                            pendingIntent
                    );
                }

                Log.d(TAG, "Đã đặt thông báo hàng ngày lúc " + hour + ":" + minute);
            } catch (SecurityException e) {
                Log.e(TAG, "Không có quyền đặt cảnh báo chính xác: " + e.getMessage());
                // Sử dụng alarm không chính xác
                alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        calendar.getTimeInMillis(),
                        pendingIntent
                );
            }
        }
    }

    /**
     * Hủy thông báo nhắc nhở hàng ngày
     */
    public static void cancelDailyReminder(Context context) {
        Intent intent = new Intent(context, DailyNotificationReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, DAILY_NOTIFICATION_ID, intent, PendingIntent.FLAG_IMMUTABLE);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
            Log.d(TAG, "Đã hủy thông báo hàng ngày");
        }
    }
}