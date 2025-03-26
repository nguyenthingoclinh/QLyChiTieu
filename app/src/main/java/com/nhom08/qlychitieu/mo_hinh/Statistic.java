package com.nhom08.qlychitieu.mo_hinh;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ForeignKey;

@Entity(tableName = "statistics",
        foreignKeys = {
                @ForeignKey(entity = User.class, parentColumns = "userID", childColumns = "userID", onDelete = ForeignKey.CASCADE),
                @ForeignKey(entity = Category.class, parentColumns = "categoryID", childColumns = "categoryID"),
        })
public class Statistic {
    @PrimaryKey(autoGenerate = true)
    private int statisticID;
    private int userID;
    private int categoryID;
    private double totalIncome;
    private double totalExpense;
    private long month;
    private long year;

    public Statistic(int userID, int categoryID, double totalIncome, double totalExpense, long month, long year) {
        this.userID = userID;
        this.categoryID = categoryID;
        this.totalIncome = totalIncome;
        this.totalExpense = totalExpense;
        this.month = month;
        this.year = year;
    }

    public int getStatisticID() {
        return statisticID;
    }

    public void setStatisticID(int statisticID) {
        this.statisticID = statisticID;
    }

    public int getUserID() {
        return userID;
    }

    public void setUserID(int userID) {
        this.userID = userID;
    }

    public double getTotalIncome() {
        return totalIncome;
    }

    public void setTotalIncome(double totalIncome) {
        this.totalIncome = totalIncome;
    }

    public long getMonth() {
        return month;
    }

    public void setMonth(long month) {
        this.month = month;
    }

    public long getYear() {
        return year;
    }

    public void setYear(long year) {
        this.year = year;
    }

    public double getTotalExpense() {
        return totalExpense;
    }

    public void setTotalExpense(double totalExpense) {
        this.totalExpense = totalExpense;
    }

    public int getCategoryID() {
        return categoryID;
    }

    public void setCategoryID(int categoryID) {
        this.categoryID = categoryID;
    }
}
