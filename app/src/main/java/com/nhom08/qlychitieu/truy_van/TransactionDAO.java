package com.nhom08.qlychitieu.truy_van;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Delete;

import com.nhom08.qlychitieu.mo_hinh.Transaction;

import java.util.List;

@Dao
public interface TransactionDAO {
    @Insert
    void insertTransaction(Transaction transaction);

    @Update
    void updateTransaction(Transaction transaction);

    @Delete
    void deleteTransaction(Transaction transaction);

    @Query("SELECT * FROM transactions WHERE userId = :userId ORDER BY date DESC LIMIT 5")
    List<Transaction> getRecentTransactions(int userId);

    @Query("SELECT * FROM transactions WHERE userId = :userId AND date BETWEEN :startDate AND :endDate")
    List<Transaction> getTransactionsByDate(int userId, long startDate, long endDate);

    @Query("SELECT * FROM transactions WHERE userId = :userId AND description LIKE '%' || :keyword || '%'")
    List<Transaction> searchTransactions(int userId, String keyword);

    @Query("SELECT SUM(amount) FROM transactions WHERE userId = :userId AND amount > 0 AND date BETWEEN :startDate AND :endDate")
    double getTotalIncome(int userId, long startDate, long endDate);

    @Query("SELECT SUM(amount) FROM transactions WHERE userId = :userId AND amount < 0 AND date BETWEEN :startDate AND :endDate")
    double getTotalExpense(int userId, long startDate, long endDate);
}
