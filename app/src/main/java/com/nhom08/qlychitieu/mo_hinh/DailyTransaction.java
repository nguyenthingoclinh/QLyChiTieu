package com.nhom08.qlychitieu.mo_hinh;

import com.nhom08.qlychitieu.tien_ich.Constants;
import java.util.ArrayList;
import java.util.List;

public class DailyTransaction {
    private final long date;
    private double totalExpense;
    private double totalIncome;
    private final List<Transaction> transactions;

    public DailyTransaction(long date) {
        this.date = date;
        this.transactions = new ArrayList<>();
        this.totalExpense = 0;
        this.totalIncome = 0;
    }

    public void addTransaction(Transaction transaction, List<Category> categories) {
        transactions.add(transaction);
        Category category = findCategoryById(categories, transaction.getCategoryId());
        if (category != null) {
            if (Constants.CATEGORY_TYPE_EXPENSE.equals(category.getType())) {
                totalExpense += Math.abs(transaction.getAmount());
            } else {
                totalIncome += transaction.getAmount();
            }
        }
    }

    private Category findCategoryById(List<Category> categories, int categoryId) {
        return categories.stream()
                .filter(c -> c.getCategoryId() == categoryId)
                .findFirst()
                .orElse(null);
    }

    public long getDate() {
        return date;
    }

    public double getTotalExpense() {
        return totalExpense;
    }

    public double getTotalIncome() {
        return totalIncome;
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }
}