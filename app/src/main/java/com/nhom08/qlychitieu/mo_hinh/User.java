package com.nhom08.qlychitieu.mo_hinh;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "users")
public class User {
    @PrimaryKey(autoGenerate = true)
    private int userId;
    private String username;
    private String password;
    private String email;
    private String fullName;
    private String googleId;
    private String resetCode;
    private String avatarPath;

    // Constructor tối thiểu
    @Ignore
    public User(String username, String password, String email, String fullName) {
        this(username, password, email, fullName, null, null, null);
    }

    // Constructor đầy đủ
    public User(String username, String password, String email, String fullName, String googleId, String resetCode, String avatarPath) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.fullName = fullName;
        this.googleId = googleId;
        this.resetCode = resetCode;
        this.avatarPath = avatarPath;
    }

    // Getter và Setter
    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getGoogleId() {
        return googleId;
    }

    public void setGoogleId(String googleId) {
        this.googleId = googleId;
    }

    public String getResetCode() {
        return resetCode;
    }

    public void setResetCode(String resetCode) {
        this.resetCode = resetCode;
    }

    public String getAvatarPath() {
        return avatarPath;
    }

    public void setAvatarPath(String avatarPath) {
        this.avatarPath = avatarPath;
    }
}