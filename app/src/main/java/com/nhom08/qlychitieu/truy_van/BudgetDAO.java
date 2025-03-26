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
    @Query("SELECT * FROM budgets WHERE userID = :userID")
    List<Budget> getBudgets(int userID);

    @Query("SELECT * FROM budgets WHERE userID = :userID AND categoryID = :categoryID")
    List<Budget> getBudgetByCategory(int userID, int categoryID);
}
