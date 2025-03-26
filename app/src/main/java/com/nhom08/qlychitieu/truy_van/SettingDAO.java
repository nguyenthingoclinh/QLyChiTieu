package com.nhom08.qlychitieu.truy_van;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Delete;

import com.nhom08.qlychitieu.mo_hinh.Setting;
@Dao
public interface SettingDAO {
    @Insert
    void insertSetting(Setting setting);

    @Update
    void updateSetting(Setting setting);

    @Delete
    void deleteSetting(Setting setting);

    @Query("SELECT * FROM settings WHERE userID = :userID LIMIT 1")
    Setting getSettingByUserID(int userID);
}
