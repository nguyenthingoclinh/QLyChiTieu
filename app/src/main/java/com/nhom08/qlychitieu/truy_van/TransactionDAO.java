package com.nhom08.qlychitieu.truy_van;
import androidx.lifecycle.LiveData;
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
    @Query("DELETE FROM transactions WHERE userId = :userId")
    void deleteAllByUserId(int userId);

    @Query("SELECT * FROM transactions WHERE userId = :userId")
    List<Transaction> getTransactionsByUserId(int userId);

    @Query("SELECT * FROM transactions WHERE userId = :userId AND date BETWEEN :startDate AND :endDate")
    List<Transaction> getTransactionsByDateRange(int userId, long startDate, long endDate);

    @Query("SELECT * FROM transactions WHERE userId = :userId AND description LIKE '%' || :keyword || '%'")
    List<Transaction> searchTransactions(int userId, String keyword);

    // Thêm class để nhận kết quả từ getDailyTotals
    class DailyTotal {
        public long date;
        public double totalIncome;
        public double totalExpense;
    }

    @Query("SELECT * FROM transactions WHERE userId = :userId AND date BETWEEN :startTime AND :endTime ORDER BY date DESC")
    LiveData<List<Transaction>> getTransactionsByDateRangeLive(int userId, long startTime, long endTime);
}
