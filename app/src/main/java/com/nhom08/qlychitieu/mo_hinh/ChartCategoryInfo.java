package com.nhom08.qlychitieu.mo_hinh;

public class ChartCategoryInfo {
    private Category category;
    private double amount;
    private int color;

    public ChartCategoryInfo(Category category, double amount, int color) {
        this.category = category;
        this.amount = amount;
        this.color = color;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public void addAmount(double amount) {
        this.amount += amount;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }
}
