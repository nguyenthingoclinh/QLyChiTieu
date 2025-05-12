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

    @Query("SELECT * FROM transactions WHERE userId = :userId AND date BETWEEN :startDate AND :endDate")
    List<Transaction> getTransactionsByDateRange(int userId, long startDate, long endDate);

    @Query("SELECT * FROM transactions WHERE userId = :userId AND description LIKE '%' || :keyword || '%'")
    List<Transaction> searchTransactions(int userId, String keyword);

    @Query("SELECT SUM(t.amount) FROM transactions t " +
            "INNER JOIN categories c ON t.categoryId = c.categoryId " +
            "WHERE t.userId = :userId AND c.type = 'Income' " +
            "AND t.date BETWEEN :startDate AND :endDate")
    Double getTotalIncome(int userId, long startDate, long endDate);

    @Query("SELECT SUM(ABS(t.amount)) FROM transactions t " +
            "INNER JOIN categories c ON t.categoryId = c.categoryId " +
            "WHERE t.userId = :userId AND c.type = 'Expense' " +
            "AND t.date BETWEEN :startDate AND :endDate")
    Double getTotalExpense(int userId, long startDate, long endDate);

    @Query("SELECT t.date as date, " +
            "SUM(CASE WHEN c.type = 'Income' THEN t.amount ELSE 0 END) as totalIncome, " +
            "SUM(CASE WHEN c.type = 'Expense' THEN ABS(t.amount) ELSE 0 END) as totalExpense " +
            "FROM transactions t " +
            "INNER JOIN categories c ON t.categoryId = c.categoryId " +
            "WHERE t.userId = :userId AND t.date BETWEEN :startDate AND :endDate " +
            "GROUP BY strftime('%Y-%m-%d', datetime(t.date/1000, 'unixepoch')) " +
            "ORDER BY t.date DESC")
    LiveData<List<DailyTotal>> getDailyTotalsLive(int userId, long startDate, long endDate);

    // Thêm class để nhận kết quả từ getDailyTotals
    class DailyTotal {
        public long date;
        public double totalIncome;
        public double totalExpense;
    }

    @Query("SELECT * FROM transactions WHERE userId = :userId AND date BETWEEN :startTime AND :endTime ORDER BY date DESC")
    LiveData<List<Transaction>> getTransactionsByDateRangeLive(int userId, long startTime, long endTime);
}
