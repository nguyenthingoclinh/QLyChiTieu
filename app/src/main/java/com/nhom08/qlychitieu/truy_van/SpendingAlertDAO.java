package com.nhom08.qlychitieu.truy_van;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Delete;

import com.nhom08.qlychitieu.mo_hinh.SpendingAlert;

import java.util.List;

@Dao
public interface SpendingAlertDAO {
    @Insert
    long insertAlert(SpendingAlert alert);

    @Update
    void updateAlert(SpendingAlert alert);

    @Delete
    void deleteAlert(SpendingAlert alert);

    @Query("SELECT * FROM spending_alerts WHERE userId = :userId ORDER BY priority DESC, alertType ASC")
    List<SpendingAlert> getAlertsByUser(int userId);

    @Query("SELECT * FROM spending_alerts WHERE userId = :userId AND active = 1 ORDER BY priority DESC")
    List<SpendingAlert> getActiveAlerts(int userId);

    @Query("SELECT * FROM spending_alerts WHERE userId = :userId AND alertType = :alertType")
    List<SpendingAlert> getAlertsByType(int userId, String alertType);

    @Query("SELECT * FROM spending_alerts WHERE alertId = :alertId")
    SpendingAlert getAlertById(int alertId);

    @Query("UPDATE spending_alerts SET active = :isActive WHERE alertId = :alertId")
    void toggleAlertActive(int alertId, boolean isActive);

    @Query("UPDATE spending_alerts SET lastNotified = :timestamp WHERE alertId = :alertId")
    void updateLastNotified(int alertId, long timestamp);

    @Query("SELECT COUNT(*) FROM spending_alerts WHERE userId = :userId")
    int getAlertCount(int userId);
}