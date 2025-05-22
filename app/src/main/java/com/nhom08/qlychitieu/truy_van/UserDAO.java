package com.nhom08.qlychitieu.truy_van;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Delete;

import com.nhom08.qlychitieu.mo_hinh.User;

import java.util.List;

@Dao
public interface UserDAO {
    @Insert
    long insertUser(User user);

    @Update
    void updateUser(User user);

    @Delete
    void deleteUser(User user);
    @Query("SELECT * FROM users")
    List<User> getAllUsers();

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    User getUserByEmail(String email);
    @Query("SELECT * FROM users WHERE userId = :id LIMIT 1")
    User getUserById(int id);

    @Query("SELECT * FROM users WHERE googleId = :googleId LIMIT 1")
    User getUserByGoogleId(String googleId);

    @Query("SELECT * FROM users WHERE email = :email AND password = :password LIMIT 1")
    User getUserByEmailAndPassword(String email, String password);

    @Query("UPDATE users SET fullName = :name WHERE email = :email")
    void updateUserName(String email, String name);

    @Query("UPDATE users SET email = :newEmail WHERE email = :oldEmail")
    void updateUserEmail(String oldEmail, String newEmail);

}
