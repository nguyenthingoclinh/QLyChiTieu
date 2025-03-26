package com.nhom08.qlychitieu.mo_hinh;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ForeignKey;

@Entity(tableName = "budgets",
foreignKeys = {
        @ForeignKey(entity = User.class, parentColumns = "userID", childColumns = "userID", onDelete = ForeignKey.CASCADE),
        @ForeignKey(entity = Category.class, parentColumns = "categoryID", childColumns = "categoryID")
})
public class Budget {
    @PrimaryKey(autoGenerate = true)
    private int budgetID;
    private int userID;
    private int categoryID;
    private double amount;
    private long startDate;
    private long endDate;

    public Budget(int userID, int categoryID, double amount, long startDate, long endDate) {
        this.userID = userID;
        this.categoryID = categoryID;
        this.amount = amount;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public int getBudgetID() {
        return budgetID;
    }

    public void setBudgetID(int budgetID) {
        this.budgetID = budgetID;
    }

    public int getUserID() {
        return userID;
    }

    public void setUserID(int userID) {
        this.userID = userID;
    }

    public int getCategoryID() {
        return categoryID;
    }

    public void setCategoryID(int categoryID) {
        this.categoryID = categoryID;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public long getStartDate() {
        return startDate;
    }

    public void setStartDate(long startDate) {
        this.startDate = startDate;
    }

    public long getEndDate() {
        return endDate;
    }

    public void setEndDate(long endDate) {
        this.endDate = endDate;
    }
}
