package com.nhom08.qlychitieu.mo_hinh;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "statistics",
        foreignKeys = {
                @ForeignKey(entity = User.class, parentColumns = "userId", childColumns = "userId", onDelete = ForeignKey.CASCADE),
                @ForeignKey(entity = Category.class, parentColumns = "categoryId", childColumns = "categoryId")
        },
        indices = {
                @Index(value = "userId"),
                @Index(value = "categoryId")
        })
public class Statistic {
    @PrimaryKey(autoGenerate = true)
    private int statisticId;
    private int userId;
    private int categoryId;
    private double totalIncome;
    private double totalExpense;
    private int month;
    private int year;

    public Statistic(int userId, int categoryId, double totalIncome, double totalExpense, int month, int year) {
        this.userId = userId;
        this.categoryId = categoryId;
        this.totalIncome = totalIncome;
        this.totalExpense = totalExpense;
        this.month = month;
        this.year = year;
    }

    public int getStatisticId() {
        return statisticId;
    }

    public void setStatisticId(int statisticId) {
        this.statisticId = statisticId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }

    public double getTotalIncome() {
        return totalIncome;
    }

    public void setTotalIncome(double totalIncome) {
        this.totalIncome = totalIncome;
    }

    public double getTotalExpense() {
        return totalExpense;
    }

    public void setTotalExpense(double totalExpense) {
        this.totalExpense = totalExpense;
    }

    public int getMonth() {
        return month;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }
}