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

    @Query("SELECT * FROM transactions WHERE userID = :userID ORDER BY date DESC LIMIT 5")
    List<Transaction> getRecentTransactions(int userID);

    @Query("SELECT * FROM transactions WHERE userID = :userID AND date BETWEEN :startDate AND :endDate")
    List<Transaction> getTransactionsByDate(int userID, long startDate, long endDate);

    @Query("SELECT * FROM transactions WHERE userID = :userID AND description LIKE '%' || :keyword || '%'")
    List<Transaction> searchTransactions(int userID, String keyword);

    @Query("SELECT SUM(amount) FROM transactions WHERE userID = :userID AND amount > 0 AND date BETWEEN :startDate AND :endDate")
    double getTotalIncome(int userID, long startDate, long endDate);

    @Query("SELECT SUM(amount) FROM transactions WHERE userID = :userID AND amount < 0 AND date BETWEEN :startDate AND :endDate")
    double getTotalExpense(int userID, long startDate, long endDate);
}
