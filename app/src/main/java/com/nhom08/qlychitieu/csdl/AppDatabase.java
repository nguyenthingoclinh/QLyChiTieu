package com.nhom08.qlychitieu.csdl;
import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.nhom08.qlychitieu.mo_hinh.Account;
import com.nhom08.qlychitieu.mo_hinh.Category;
import com.nhom08.qlychitieu.mo_hinh.Reminder;
import com.nhom08.qlychitieu.mo_hinh.Setting;
import com.nhom08.qlychitieu.mo_hinh.Statistic;
import com.nhom08.qlychitieu.mo_hinh.Transaction;
import com.nhom08.qlychitieu.mo_hinh.User;
import com.nhom08.qlychitieu.truy_van.AccountDAO;
import com.nhom08.qlychitieu.truy_van.CategoryDAO;
import com.nhom08.qlychitieu.truy_van.ReminderDAO;
import com.nhom08.qlychitieu.truy_van.SettingDAO;
import com.nhom08.qlychitieu.truy_van.StatisticDAO;
import com.nhom08.qlychitieu.truy_van.TransactionDAO;
import com.nhom08.qlychitieu.truy_van.UserDAO;

@Database(entities = {User.class, Setting.class, Category.class, Account.class, Transaction.class, Statistic.class, Reminder.class}, version = 2, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    public abstract UserDAO userDao();
    public abstract SettingDAO settingDao();
    public abstract CategoryDAO categoryDao();
    public abstract AccountDAO accountDao();
    public abstract TransactionDAO transactionDao();
    public abstract StatisticDAO statisticDao();
    public abstract ReminderDAO reminderDao();

    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("DROP TABLE IF EXISTS Budget");
        }
    };
}
