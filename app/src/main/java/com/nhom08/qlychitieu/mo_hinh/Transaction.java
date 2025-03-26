package com.nhom08.qlychitieu.mo_hinh;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ForeignKey;

@Entity(tableName = "transactions",
        foreignKeys = {
            @ForeignKey(entity = User.class, parentColumns = "userID", childColumns = "userID", onDelete = ForeignKey.CASCADE),
            @ForeignKey(entity = Category.class, parentColumns = "categoryID", childColumns = "categoryID"),
            @ForeignKey(entity = BankAccount.class, parentColumns = "bankAccountID", childColumns = "bankAccountID")
        })
public class Transaction {
        @PrimaryKey(autoGenerate = true)
        private int transactionID;
        private int userID;
        private int categoryID;
        private int bankAccountID;
        private double amount; //Số tiền giao dịch
        private long date;
        private String description;
        private String imagePath;

        public Transaction(int userID, int categoryID, int bankAccountID, double amount, long date, String description, String imagePath) {
                this.userID = userID;
                this.categoryID = categoryID;
                this.bankAccountID = bankAccountID;
                this.amount = amount;
                this.date = date;
                this.description = description;
                this.imagePath = imagePath;
        }

        public int getTransactionID() {
                return transactionID;
        }

        public void setTransactionID(int transactionID) {
                this.transactionID = transactionID;
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

        public int getBankAccountID() {
                return bankAccountID;
        }

        public void setBankAccountID(int bankAccountID) {
                this.bankAccountID = bankAccountID;
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
