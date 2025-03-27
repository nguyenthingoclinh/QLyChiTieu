package com.nhom08.qlychitieu.truy_van;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Delete;

import com.nhom08.qlychitieu.mo_hinh.Reminder;

import java.util.List;

@Dao
public interface ReminderDAO {
    @Insert
    void insertReminder(Reminder reminder);
    @Update
    void updateReminder(Reminder reminder);
    @Delete
    void deleteReminder(Reminder reminder);
    @Query("SELECT * FROM reminders WHERE userId = :userId")
    List<Reminder> getReminders(int userId);
}
