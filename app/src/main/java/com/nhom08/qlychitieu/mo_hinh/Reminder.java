package com.nhom08.qlychitieu.mo_hinh;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "reminders",
        foreignKeys = @ForeignKey(entity = User.class, parentColumns = "userId", childColumns = "userId", onDelete = ForeignKey.CASCADE),
        indices = {@Index(value = "userId")})
public class Reminder {
    @PrimaryKey(autoGenerate = true)
    private int reminderId;
    private int userId;
    private String title; // Tiêu đề
    private long time; // Thời gian nhắc nhở
    private String repeat; // Tần suất lặp lại lời nhắc

    public Reminder(int userId, String title, long time, String repeat) {
        this.userId = userId;
        this.title = title;
        this.time = time;
        this.repeat = repeat;
    }

    public int getReminderId() {
        return reminderId;
    }

    public void setReminderId(int reminderId) {
        this.reminderId = reminderId;
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

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public String getRepeat() {
        return repeat;
    }

    public void setRepeat(String repeat) {
        this.repeat = repeat;
    }
}