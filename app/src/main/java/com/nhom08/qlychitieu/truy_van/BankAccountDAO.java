package com.nhom08.qlychitieu.truy_van;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Delete;

import com.nhom08.qlychitieu.mo_hinh.BankAccount;

import java.util.List;
@Dao
public interface BankAccountDAO {
    @Insert
    void insertBankAccount(BankAccount bankAccount);
    @Update
    void updateBankAccount(BankAccount bankAccount);
    @Delete
    void deleteBankAccount(BankAccount bankAccount);
    @Query("SELECT * FROM bank_accounts WHERE userID = :userID")
    List<BankAccount> getBankAccounts(int userID);
}
