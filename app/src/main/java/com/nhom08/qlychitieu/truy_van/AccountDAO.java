package com.nhom08.qlychitieu.truy_van;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Delete;

import com.nhom08.qlychitieu.mo_hinh.Account;

import java.util.List;
@Dao
public interface AccountDAO {
    @Insert
    void insertBankAccount(Account account);
    @Update
    void updateBankAccount(Account account);
    @Delete
    void deleteBankAccount(Account account);
    @Query("SELECT * FROM accounts WHERE userId = :userId")
    List<Account> getBankAccounts(int userId);
}
