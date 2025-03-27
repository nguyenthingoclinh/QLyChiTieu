package com.nhom08.qlychitieu.truy_van;
import androidx.room.Dao;
import androidx.room.Query;
import androidx.room.Insert;
import androidx.room.Update;
import androidx.room.Delete;

import com.nhom08.qlychitieu.mo_hinh.Statistic;

import java.util.List;

@Dao
public interface StatisticDAO {
    @Insert
    void insertStatistic(Statistic statistic);
    @Update
    void updateStatistic(Statistic statistic);
    @Delete
    void deleteStatistic(Statistic statistic);
    @Query("SELECT * FROM statistics WHERE userId = :userId AND month = :month AND year = :year")
    List<Statistic> getStatisticsByMonth(int userId, int month, int year);

    @Query("SELECT * FROM statistics WHERE userId = :userId AND categoryID = :categoryID")
    List<Statistic> getStatisticsByCategoryAndMonth(int userId, int categoryID);

}
