package com.nhom08.qlychitieu.mo_hinh;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "settings",
        foreignKeys = @ForeignKey(entity = User.class, parentColumns = "userId", childColumns = "userId", onDelete = ForeignKey.CASCADE),
        indices = {@Index(value = "userId")})
public class Setting {
    @PrimaryKey
    private int userId;
    private String theme;
    private String numberFormat;
    private String currency;

    public Setting(int userId, String currency, String numberFormat, String theme) {
        this.userId = userId;
        this.currency = currency;
        this.numberFormat = numberFormat;
        this.theme = theme;
    }

    @Ignore
    public Setting(int userId) {
        this(userId, "VND", "1,234.56", "System");
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    public String getNumberFormat() {
        return numberFormat;
    }

    public void setNumberFormat(String numberFormat) {
        this.numberFormat = numberFormat;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }
}