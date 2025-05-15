package com.nhom08.qlychitieu.mo_hinh;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "spending_alerts",
        foreignKeys = @ForeignKey(entity = User.class, parentColumns = "userId", childColumns = "userId", onDelete = ForeignKey.CASCADE),
        indices = {@Index(value = "userId")})
public class SpendingAlert {
    @PrimaryKey(autoGenerate = true)
    private int alertId;
    private int userId;
    private String title;                 // Tiêu đề cảnh báo
    private String alertType;             // Loại cảnh báo: "DAILY", "WEEKLY", "MONTHLY"
    private double threshold;             // Ngưỡng phần trăm (ví dụ: 80 = cảnh báo khi chi tiêu đạt 80% thu nhập)
    private boolean active;               // Trạng thái kích hoạt
    private long lastNotified;            // Thời gian thông báo gần nhất
    private String notifyTime;            // Thời gian thông báo trong ngày (HH:mm)
    private int priority;                 // Mức độ ưu tiên (1: Thấp, 2: Trung bình, 3: Cao)


    public SpendingAlert(){
    }
    @Ignore
    public SpendingAlert(int userId, String title, String alertType, double threshold, boolean active, String notifyTime, int priority) {
        this.userId = userId;
        this.title = title;
        this.alertType = alertType;
        this.threshold = threshold;
        this.active = active;
        this.notifyTime = notifyTime;
        this.priority = priority;
        this.lastNotified = 0; // Chưa thông báo bao giờ
    }

    // Constructor đơn giản hơn
    @Ignore
    public SpendingAlert(int userId, String title, String alertType, double threshold) {
        this(userId, title, alertType, threshold, true, "21:00", 2);
    }

    // Getters and Setters
    public int getAlertId() {
        return alertId;
    }

    public void setAlertId(int alertId) {
        this.alertId = alertId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAlertType() {
        return alertType;
    }

    public void setAlertType(String alertType) {
        this.alertType = alertType;
    }

    public double getThreshold() {
        return threshold;
    }

    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public long getLastNotified() {
        return lastNotified;
    }

    public void setLastNotified(long lastNotified) {
        this.lastNotified = lastNotified;
    }

    public String getNotifyTime() {
        return notifyTime;
    }

    public void setNotifyTime(String notifyTime) {
        this.notifyTime = notifyTime;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }
}