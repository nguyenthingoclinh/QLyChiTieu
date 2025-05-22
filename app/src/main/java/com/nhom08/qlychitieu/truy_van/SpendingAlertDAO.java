package com.nhom08.qlychitieu.truy_van;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.nhom08.qlychitieu.mo_hinh.SpendingAlert;

import java.util.List;

@Dao
public interface SpendingAlertDAO {

    @Insert
    long insert(SpendingAlert alert);

    @Update
    void update(SpendingAlert alert);

    @Delete
    void delete(SpendingAlert alert);
    @Query("DELETE FROM spending_alerts WHERE userId = :userId")
    void deleteAllByUserId(int userId);

    @Query("SELECT * FROM spending_alerts WHERE alertId = :alertId")
    SpendingAlert getById(int alertId);

    @Query("SELECT * FROM spending_alerts WHERE userId = :userId ORDER BY alertType, title")
    List<SpendingAlert> getByUserId(int userId);

    @Query("SELECT * FROM spending_alerts WHERE userId = :userId AND active = 1 ORDER BY alertType, title")
    List<SpendingAlert> getActiveAlertsByUserId(int userId);

    @Query("SELECT * FROM spending_alerts WHERE userId = :userId AND alertType = :alertType")
    List<SpendingAlert> getByUserIdAndType(int userId, String alertType);

    @Query("UPDATE spending_alerts SET active = :active WHERE alertId = :alertId")
    void updateActiveStatus(int alertId, boolean active);
}