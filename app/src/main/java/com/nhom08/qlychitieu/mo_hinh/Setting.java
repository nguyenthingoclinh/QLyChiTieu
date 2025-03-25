package com.nhom08.qlychitieu.mo_hinh;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ForeignKey;

@Entity(tableName = "settings",
foreignKeys = @ForeignKey(entity = User.class, parentColumns = "userID", childColumns = "userID", onDelete = ForeignKey.CASCADE))
public class Setting {
    @PrimaryKey
    private int UserID;
    private String theme;
    private String numberFormat;
    private String currency;

    public Setting(int userID, String currency, String numberFormat, String theme) {
        UserID = userID;
        this.currency = currency;
        this.numberFormat = numberFormat;
        this.theme = theme;
    }

    public Setting(int userID) {
        this(userID, "VND", "1,234.56", "System");
    }

    public int getUserID() {
        return UserID;
    }

    public void setUserID(int userID) {
        UserID = userID;
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
