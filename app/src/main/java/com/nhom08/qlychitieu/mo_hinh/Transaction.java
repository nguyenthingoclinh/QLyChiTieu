package com.nhom08.qlychitieu.mo_hinh;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "transactions",
        foreignKeys = {
                @ForeignKey(entity = User.class, parentColumns = "userId", childColumns = "userId", onDelete = ForeignKey.CASCADE),
                @ForeignKey(entity = Category.class, parentColumns = "categoryId", childColumns = "categoryId"),
                @ForeignKey(entity = Account.class, parentColumns = "accountId", childColumns = "accountId")
        },
        indices = {
                @Index(value = "userId"),
                @Index(value = "categoryId"),
                @Index(value = "accountId")
        })
public class Transaction {
        @PrimaryKey(autoGenerate = true)
        private int transactionId;
        private int userId;
        private Integer categoryId;
        private Integer accountId;
        private double amount; // Số tiền giao dịch
        private long date;
        private String description;
        private String imagePath;

        public Transaction(int userId, Integer categoryId, Integer accountId, double amount, long date, String description, String imagePath) {
                this.userId = userId;
                this.categoryId = categoryId;
                this.accountId = accountId;
                this.amount = amount;
                this.date = date;
                this.description = description;
                this.imagePath = imagePath;
        }

        public int getTransactionId() {
                return transactionId;
        }

        public void setTransactionId(int transactionId) {
                this.transactionId = transactionId;
        }

        public int getUserId() {
                return userId;
        }

        public void setUserId(int userId) {
                this.userId = userId;
        }

        public Integer getCategoryId() {
                return categoryId;
        }

        public void setCategoryId(Integer categoryId) {
                this.categoryId = categoryId;
        }

        public Integer getAccountId() {
                return accountId;
        }

        public void setAccountId(Integer accountId) {
                this.accountId = accountId;
        }

        public double getAmount() {
                return amount;
        }

        public void setAmount(double amount) {
                this.amount = amount;
        }

        public long getDate() {
                return date;
        }

        public void setDate(long date) {
                this.date = date;
        }

        public String getDescription() {
                return description;
        }

        public void setDescription(String description) {
                this.description = description;
        }

        public String getImagePath() {
                return imagePath;
        }

        public void setImagePath(String imagePath) {
                this.imagePath = imagePath;
        }
}