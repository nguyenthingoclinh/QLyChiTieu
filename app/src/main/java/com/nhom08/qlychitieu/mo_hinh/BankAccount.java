package com.nhom08.qlychitieu.mo_hinh;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ForeignKey;

@Entity(tableName = "bank_accounts",
foreignKeys = @ForeignKey(entity = User.class, parentColumns = "userID", childColumns = "userID", onDelete = ForeignKey.CASCADE))
public class BankAccount {
    @PrimaryKey(autoGenerate = true)
    private int bankAccountID;
    private int userID;
    private String name;
    private double balance;
    private String icon;
    private String type;
    private String description;
    private String currency;

    public BankAccount(int userID, String name, double balance, String icon, String type, String description, String currency) {
        this.userID = userID;
        this.name = name;
        this.balance = balance;
        this.icon = icon;
        this.type = type;
        this.description = description;
        this.currency = currency;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getBankAccountID() {
        return bankAccountID;
    }

    public void setBankAccountID(int bankAccountID) {
        this.bankAccountID = bankAccountID;
    }

    public int getUserID() {
        return userID;
    }

    public void setUserID(int userID) {
        this.userID = userID;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }
}
