package com.nhom08.qlychitieu.truy_van;
import androidx.room.Dao;
import androidx.room.Query;
import androidx.room.Insert;
import androidx.room.Update;
import androidx.room.Delete;

import com.nhom08.qlychitieu.mo_hinh.Budget;

import java.util.List;

@Dao
public interface BudgetDAO {
    @Insert
    void insertBudget(Budget budget);
    @Update
    void updateBudget(Budget budget);
    @Delete
    void deleteBudget(Budget budget);
    @Query("SELECT * FROM budgets WHERE userId = :userId")
    List<Budget> getBudgets(int userId);

    @Query("SELECT * FROM budgets WHERE userId = :userId AND categoryId = :categoryId")
    List<Budget> getBudgetByCategory(int userId, int categoryId);
}
