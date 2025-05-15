package com.nhom08.qlychitieu.tien_ich;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.nhom08.qlychitieu.R;
import com.nhom08.qlychitieu.giao_dien.man_hinh_chinh.MainActivity;
import com.nhom08.qlychitieu.mo_hinh.SpendingAlert;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class NotificationHelper {

    private static final String CHANNEL_ID = "spending_alerts";
    private static final String CHANNEL_NAME = "Cảnh báo chi tiêu";
    private static final String CHANNEL_DESC = "Thông báo cảnh báo về chi tiêu vượt ngưỡng";

    /**
     * Tạo channel thông báo (chỉ cần làm một lần)
     * @param context Context để tạo notification channel
     */
    public static void createNotificationChannel(Context context) {
        // Chỉ cần thiết từ Android 8.0 (API 26) trở lên
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT);

            channel.setDescription(CHANNEL_DESC);

            // Đăng ký channel với hệ thống
            NotificationManager notificationManager =
                    context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    /**
     * Hiển thị thông báo chi tiêu
     * @param context Context để hiển thị thông báo
     * @param alert Đối tượng cảnh báo chứa thông tin
     * @param spendingPercentage Phần trăm chi tiêu so với thu nhập
     * @return true nếu hiển thị thành công, false nếu không đủ quyền
     */
    public static boolean showSpendingAlert(Context context, SpendingAlert alert, double spendingPercentage) {
        // Tạo intent khi người dùng nhấp vào thông báo
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, flags);

        // Định dạng phần trăm
        String percentText = String.format(Locale.getDefault(), "%.1f%%", spendingPercentage);

        // Tạo nội dung thông báo dựa vào loại cảnh báo
        String contentText;
        if ("DAILY".equals(alert.getAlertType())) {
            contentText = "Hôm nay bạn đã chi tiêu " + percentText + " của thu nhập hàng ngày.";
        } else if ("WEEKLY".equals(alert.getAlertType())) {
            contentText = "Tuần này bạn đã chi tiêu " + percentText + " của thu nhập hàng tuần.";
        } else {
            contentText = "Tháng này bạn đã chi tiêu " + percentText + " của thu nhập hàng tháng.";
        }

        // Xác định mức độ ưu tiên của thông báo
        int priority;
        int smallIcon;

        if (spendingPercentage >= 100) {
            priority = NotificationCompat.PRIORITY_HIGH;
            smallIcon = R.drawable.ic_warning; // Cần tạo icon này trong res/drawable
        } else if (spendingPercentage >= 90) {
            priority = NotificationCompat.PRIORITY_DEFAULT;
            smallIcon = R.drawable.ic_alert; // Cần tạo icon này trong res/drawable
        } else {
            priority = NotificationCompat.PRIORITY_LOW;
            smallIcon = R.drawable.ic_info; // Cần tạo icon này trong res/drawable
        }

        // Xây dựng thông báo
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(smallIcon)
                .setContentTitle(alert.getTitle())
                .setContentText(contentText)
                .setPriority(priority)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        // Hiển thị thông báo
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

        // Kiểm tra quyền trước khi hiển thị thông báo
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                return false; // Không có quyền hiển thị thông báo
            }
        }

        notificationManager.notify(alert.getAlertId(), builder.build());

        // Cập nhật thời gian thông báo gần nhất
        alert.setLastNotified(System.currentTimeMillis());
        return true;
    }
}