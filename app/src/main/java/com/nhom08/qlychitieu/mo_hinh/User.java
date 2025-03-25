package com.nhom08.qlychitieu.mo_hinh;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "users")
public class User {
    @PrimaryKey(autoGenerate = true)
    private int userID;
    private String username;
    private String password;
    private String email;
    private String fullName;
    private String googleID;
    private String resetCode;
    private String avatarPath;

    //Constructor tối thiếu
    public User(String username, String password, String email, String fullName) {
        this(username, password, email, fullName,null,null,null);
    }

    //Constructor đầy đủ
    public User(String username, String password, String email, String fullName, String googleID, String resetCode, String avatarPath) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.fullName = fullName;
        this.googleID = googleID;
        this.resetCode = resetCode;
        this.avatarPath = avatarPath;
    }

    public int getUserID() {
        return userID;
    }

    public void setUserID(int userID) {
        this.userID = userID;
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

    public String getAvatarPath() {
        return avatarPath;
    }

    public void setAvatarPath(String avatarPath) {
        this.avatarPath = avatarPath;
    }

    public String getResetCode() {
        return resetCode;
    }

    public void setResetCode(String resetCode) {
        this.resetCode = resetCode;
    }

    public String getGoogleID() {
        return googleID;
    }

    public void setGoogleID(String googleID) {
        this.googleID = googleID;
    }
}
