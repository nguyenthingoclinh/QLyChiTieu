package com.nhom08.qlychitieu.csdl;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.nhom08.qlychitieu.mo_hinh.Category;
import com.nhom08.qlychitieu.mo_hinh.SpendingAlert;
import com.nhom08.qlychitieu.mo_hinh.Transaction;
import com.nhom08.qlychitieu.mo_hinh.User;
import com.nhom08.qlychitieu.truy_van.CategoryDAO;
import com.nhom08.qlychitieu.truy_van.SpendingAlertDAO;
import com.nhom08.qlychitieu.truy_van.TransactionDAO;
import com.nhom08.qlychitieu.truy_van.UserDAO;

@Database(entities = {User.class, Category.class, Transaction.class, SpendingAlert.class}, version = 3, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    public abstract UserDAO userDao();
    public abstract CategoryDAO categoryDao();
    public abstract TransactionDAO transactionDao();
    public abstract SpendingAlertDAO spendingAlertDao();

    // Migration cũ từ version 1 đến 2
    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("DROP TABLE IF EXISTS Budget");
        }
    };

    // Migration mới từ version 2 đến 3 với sao lưu dữ liệu đầy đủ
    public static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // ===== PHẦN 1: SAO LƯU USER =====
            // 1. Tạo bảng users_backup để sao lưu dữ liệu users
            database.execSQL("CREATE TABLE users_backup (" +
                    "userId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "fullName TEXT, " +
                    "email TEXT, " +
                    "password TEXT, " +
                    "googleId TEXT, " +
                    "resetCode TEXT, " +
                    "avatarPath TEXT" +
                    ")");

            // 2. Sao chép dữ liệu từ users sang users_backup (không còn lưu numberFormat và currency)
            database.execSQL("INSERT INTO users_backup(" +
                    "userId, fullName, email, password, googleId, resetCode, avatarPath) " +
                    "SELECT userId, fullName, email, password, googleId, resetCode, avatarPath FROM users");

            // 3. Xóa bảng users cũ
            database.execSQL("DROP TABLE users");

            // 4. Đổi tên users_backup thành users
            database.execSQL("ALTER TABLE users_backup RENAME TO users");

            // 5. Tạo lại các chỉ mục (indexes) cho bảng users nếu cần
            // database.execSQL("CREATE INDEX IF NOT EXISTS index_users_email ON users(email)");

            // ===== PHẦN 2: SAO LƯU CATEGORY =====
            // 1. Tạo bảng categories_backup để sao lưu dữ liệu categories
            database.execSQL("CREATE TABLE categories_backup (" +
                    "categoryId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "userId INTEGER NOT NULL, " +
                    "name TEXT NOT NULL, " +
                    "type TEXT NOT NULL, " +
                    "iconName TEXT, " +
                    "FOREIGN KEY(userId) REFERENCES users(userId) ON DELETE CASCADE" +
                    ")");

            // 2. Sao chép dữ liệu từ categories sang categories_backup
            database.execSQL("INSERT INTO categories_backup " +
                    "SELECT * FROM categories");

            // 3. Xóa bảng categories cũ
            database.execSQL("DROP TABLE categories");

            // 4. Đổi tên categories_backup thành categories
            database.execSQL("ALTER TABLE categories_backup RENAME TO categories");

            // 5. Tạo lại các chỉ mục (indexes) cho bảng categories
            database.execSQL("CREATE INDEX IF NOT EXISTS index_categories_userId ON categories(userId)");

            // ===== PHẦN 3: SAO LƯU VÀ CẬP NHẬT TRANSACTIONS =====
            // 1. Tạo bảng transactions_new không có cột accountId
            database.execSQL("CREATE TABLE transactions_new (" +
                    "transactionId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "userId INTEGER NOT NULL, " +
                    "categoryId INTEGER, " +
                    "amount REAL NOT NULL, " +
                    "date INTEGER NOT NULL, " +
                    "description TEXT, " +
                    "imagePath TEXT, " +
                    "FOREIGN KEY(userId) REFERENCES users(userId) ON DELETE CASCADE, " +
                    "FOREIGN KEY(categoryId) REFERENCES categories(categoryId)" +
                    ")");

            // 2. Sao chép dữ liệu từ transactions sang transactions_new (không bao gồm cột accountId)
            database.execSQL("INSERT INTO transactions_new(" +
                    "transactionId, userId, categoryId, amount, date, description, imagePath) " +
                    "SELECT transactionId, userId, categoryId, amount, date, description, imagePath FROM transactions");

            // 3. Xóa bảng transactions cũ
            database.execSQL("DROP TABLE transactions");

            // 4. Đổi tên transactions_new thành transactions
            database.execSQL("ALTER TABLE transactions_new RENAME TO transactions");

            // 5. Tạo lại các chỉ mục (indexes) cho bảng transactions
            database.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_userId ON transactions(userId)");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_categoryId ON transactions(categoryId)");

            // ===== PHẦN 4: TẠO BẢNG SPENDING_ALERTS MỚI =====
            // 1. Tạo bảng spending_alerts
            database.execSQL("CREATE TABLE spending_alerts (" +
                    "alertId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "userId INTEGER NOT NULL, " +
                    "title TEXT NOT NULL, " +
                    "alertType TEXT NOT NULL DEFAULT 'DAILY', " +
                    "threshold REAL NOT NULL DEFAULT 80.0, " +
                    "active INTEGER NOT NULL DEFAULT 1, " +
                    "lastNotified INTEGER NOT NULL DEFAULT 0, " +
                    "notifyTime TEXT NOT NULL DEFAULT '21:00', " +
                    "priority INTEGER NOT NULL DEFAULT 2, " +
                    "FOREIGN KEY(userId) REFERENCES users(userId) ON DELETE CASCADE" +
                    ")");

            // 2. Sao chép dữ liệu từ reminders sang spending_alerts nếu bảng reminders tồn tại
            try {
                database.execSQL("INSERT INTO spending_alerts (userId, title, alertType) " +
                        "SELECT userId, title, 'DAILY' FROM reminders");
            } catch (Exception e) {
                // Bỏ qua nếu không có bảng reminders hoặc lỗi khi sao chép
            }

            // 3. Tạo chỉ mục (index) cho bảng spending_alerts
            database.execSQL("CREATE INDEX IF NOT EXISTS index_spending_alerts_userId ON spending_alerts(userId)");

            // ===== PHẦN 5: XÓA CÁC BẢNG KHÔNG CẦN THIẾT =====
            database.execSQL("DROP TABLE IF EXISTS reminders");
            database.execSQL("DROP TABLE IF EXISTS statistics");
            database.execSQL("DROP TABLE IF EXISTS accounts");
            database.execSQL("DROP TABLE IF EXISTS settings");
        }
    };
    private static AppDatabase INSTANCE;

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "your_database_name")
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3) // nếu cần
                    .fallbackToDestructiveMigration() // xóa database nếu không migrate được
                    .build();
        }
        return INSTANCE;
    }
}