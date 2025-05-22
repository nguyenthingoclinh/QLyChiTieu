package com.nhom08.qlychitieu.tien_ich;

import android.content.Context;
import android.util.Log;

import com.nhom08.qlychitieu.csdl.AppDatabase;
import com.nhom08.qlychitieu.mo_hinh.Category;
import com.nhom08.qlychitieu.mo_hinh.SpendingAlert;
import com.nhom08.qlychitieu.mo_hinh.Transaction;
import com.nhom08.qlychitieu.mo_hinh.User;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Helper class để xử lý xuất và nhập dữ liệu
 */
public class ExportImportHelper {
    private static final String TAG = "ExportImportHelper";
    private static final String EXPORT_FILE_NAME = "qlychitieu_data";
    private static final String EXPORT_FILE_EXT = ".json";

    /**
     * Xuất dữ liệu vào bộ nhớ ngoài và trả về file
     */
    public static File exportData(Context context, AppDatabase database, int userId) throws Exception {
        // Tạo tên file với timestamp
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        String timestamp = sdf.format(new Date());
        String fileName = EXPORT_FILE_NAME + "_" + timestamp + EXPORT_FILE_EXT;

        // Tạo file trong bộ nhớ ngoài
        File exportDir = context.getExternalFilesDir(null);
        assert exportDir != null;
        if (!exportDir.exists()) {
            exportDir.mkdirs();
        }
        File exportFile = new File(exportDir, fileName);

        // Thu thập dữ liệu từ database
        List<Category> categories = database.categoryDao().getCategoryByUserId(userId);
        List<Transaction> transactions = database.transactionDao().getTransactionsByUserId(userId);
        List<SpendingAlert> alerts = database.spendingAlertDao().getByUserId(userId);
        User user = database.userDao().getUserById(userId);

        // Tạo đối tượng JSON để lưu dữ liệu
        JSONObject data = new JSONObject();

        try {
            // Thêm thông tin phiên bản và thời gian xuất
            data.put("version", "1.0");
            data.put("timestamp", System.currentTimeMillis());

            // Thêm thông tin cần thiết của người dùng (chỉ email và fullName)
            JSONObject userObject = new JSONObject();
            userObject.put("userId", user.getUserId());
            userObject.put("email", user.getEmail());
            userObject.put("fullName", user.getFullName());

            // Thêm googleId nếu là tài khoản Google
            String googleId = user.getGoogleId();
            if (googleId != null && !googleId.isEmpty()) {
                userObject.put("googleId", googleId);
            }

            data.put("user", userObject);

            // Thêm danh mục
            data.put("categories", convertCategoriesToJson(categories));

            // Thêm giao dịch
            data.put("transactions", convertTransactionsToJson(transactions));

            // Thêm cảnh báo
            data.put("alerts", convertAlertsToJson(alerts));

            // Ghi dữ liệu vào file
            try (FileOutputStream fos = new FileOutputStream(exportFile);
                 OutputStreamWriter writer = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {
                writer.write(data.toString(4));
                writer.flush();
            }

            return exportFile;
        } catch (JSONException e) {
            Log.e(TAG, "Lỗi khi tạo JSON: " + e.getMessage());
            throw new Exception("Lỗi khi tạo dữ liệu JSON: " + e.getMessage());
        }
    }

    /**
     * Nhập dữ liệu từ file
     */
    public static boolean importData(Context context, AppDatabase database, File importFile, int userId) throws Exception {
        try (FileInputStream fis = new FileInputStream(importFile);
             BufferedReader reader = new BufferedReader(new InputStreamReader(fis, StandardCharsets.UTF_8))) {

            StringBuilder jsonBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonBuilder.append(line);
            }

            String jsonData = jsonBuilder.toString();
            JSONObject data = new JSONObject(jsonData);

            // Kiểm tra phiên bản
            String version = data.getString("version");
            if (!"1.0".equals(version)) {
                throw new Exception("Phiên bản file không tương thích: " + version);
            }

            // Kiểm tra thông tin người dùng
            JSONObject userObject = data.getJSONObject("user");
            User currentUser = database.userDao().getUserById(userId);

            // Kiểm tra bằng email thay vì ID
            boolean isValidUser = false;

            // Nếu là tài khoản Google, kiểm tra googleId
            if (currentUser.getGoogleId() != null && !currentUser.getGoogleId().isEmpty()) {
                if (userObject.has("googleId")) {
                    String backupGoogleId = userObject.getString("googleId");
                    if (currentUser.getGoogleId().equals(backupGoogleId)) {
                        isValidUser = true;
                    } else {
                        throw new Exception("File sao lưu thuộc về tài khoản Google khác");
                    }
                } else {
                    throw new Exception("File sao lưu không phải từ tài khoản Google");
                }
            }
            // Kiểm tra email cho các tài khoản thông thường
            else if (userObject.has("email")) {
                String backupEmail = userObject.getString("email");
                if (currentUser.getEmail().equals(backupEmail)) {
                    isValidUser = true;
                } else {
                    throw new Exception("File sao lưu thuộc về tài khoản khác (email khác nhau)");
                }
            } else {
                throw new Exception("Không tìm thấy thông tin tài khoản trong file sao lưu");
            }

            if (!isValidUser) {
                throw new Exception("Không thể xác thực người dùng từ file sao lưu");
            }

            // Bắt đầu nhập dữ liệu trong một giao dịch
            database.runInTransaction(() -> {
                try {
                    // Xóa dữ liệu hiện có của người dùng
                    database.spendingAlertDao().deleteAllByUserId(userId);
                    database.transactionDao().deleteAllByUserId(userId);
                    database.categoryDao().deleteAllByUserId(userId);

                    // Tạo map để lưu trữ ánh xạ giữa categoryId cũ và mới
                    Map<Integer, Integer> categoryIdMap = new HashMap<>();

                    // Khôi phục danh mục
                    if (data.has("categories")) {
                        JSONArray categoriesArray = data.getJSONArray("categories");
                        for (int i = 0; i < categoriesArray.length(); i++) {
                            JSONObject categoryObject = categoriesArray.getJSONObject(i);

                            // Lưu lại categoryId cũ để ánh xạ sau này
                            int oldCategoryId = categoryObject.getInt("categoryId");

                            // Tạo category mới và thiết lập các thuộc tính
                            Category category = new Category();
                            category.setName(categoryObject.getString("name"));
                            category.setIcon(categoryObject.getString("icon"));
                            category.setType(categoryObject.getString("type"));
                            category.setUserId(userId); // Đảm bảo đặt userId hiện tại

                            // Insert và lấy ID mới
                            long newCategoryId = database.categoryDao().insertCategory(category);

                            // Lưu ánh xạ giữa ID cũ và ID mới
                            categoryIdMap.put(oldCategoryId, (int) newCategoryId);
                        }
                    }

                    // Khôi phục giao dịch
                    if (data.has("transactions")) {
                        JSONArray transactionsArray = data.getJSONArray("transactions");
                        for (int i = 0; i < transactionsArray.length(); i++) {
                            JSONObject transactionObject = transactionsArray.getJSONObject(i);

                            // Tạo transaction mới
                            Transaction transaction = new Transaction();
                            transaction.setAmount(transactionObject.getDouble("amount"));

                            if (transactionObject.has("description")) {
                                transaction.setDescription(transactionObject.getString("description"));
                            }

                            transaction.setDate(transactionObject.getLong("date"));

                            // Lấy categoryId cũ và áp dụng ánh xạ
                            int oldCategoryId = transactionObject.getInt("categoryId");
                            if (categoryIdMap.containsKey(oldCategoryId)) {
                                transaction.setCategoryId(categoryIdMap.get(oldCategoryId));
                            } else {
                                // Nếu không tìm thấy trong map, dùng categoryId đầu tiên hoặc 0
                                if (!categoryIdMap.isEmpty()) {
                                    transaction.setCategoryId(categoryIdMap.values().iterator().next());
                                } else {
                                    transaction.setCategoryId(0);
                                }
                            }

                            transaction.setUserId(userId);

                            // Insert vào database
                            database.transactionDao().insertTransaction(transaction);
                        }
                    }

                    // Khôi phục cảnh báo
                    if (data.has("alerts")) {
                        JSONArray alertsArray = data.getJSONArray("alerts");
                        for (int i = 0; i < alertsArray.length(); i++) {
                            JSONObject alertObject = alertsArray.getJSONObject(i);

                            try {
                                // Tạo alert mới
                                SpendingAlert alert = new SpendingAlert();

                                if (alertObject.has("title")) {
                                    alert.setTitle(alertObject.getString("title"));
                                }

                                if (alertObject.has("type")) {
                                    alert.setAlertType(alertObject.getString("type"));
                                }

                                if (alertObject.has("threshold")) {
                                    alert.setThreshold(alertObject.getDouble("threshold"));
                                }

                                if (alertObject.has("priority")) {
                                    alert.setPriority(alertObject.getInt("priority"));
                                }

                                if (alertObject.has("active")) {
                                    alert.setActive(alertObject.getBoolean("active"));
                                }

                                if (alertObject.has("lastNotified")) {
                                    alert.setLastNotified(alertObject.getLong("lastNotified"));
                                }

                                if (alertObject.has("notifyTime")) {
                                    alert.setNotifyTime(String.valueOf(alertObject.getLong("notifyTime")));
                                }

                                alert.setUserId(userId);

                                // Insert vào database
                                database.spendingAlertDao().insert(alert);
                            } catch (JSONException e) {
                                Log.e(TAG, "Lỗi khi xử lý alert: " + e.getMessage());
                                // Tiếp tục với alert tiếp theo
                            }
                        }
                    }
                } catch (JSONException e) {
                    throw new RuntimeException("Lỗi khi xử lý dữ liệu JSON: " + e.getMessage());
                }
            });

            return true;
        } catch (JSONException e) {
            Log.e(TAG, "Lỗi khi đọc JSON: " + e.getMessage(), e);
            throw new Exception("Lỗi khi đọc dữ liệu JSON: " + e.getMessage());
        }
    }

    // Phương thức chuyển đổi danh sách Category thành JSONArray
    private static JSONArray convertCategoriesToJson(List<Category> categories) throws JSONException {
        JSONArray jsonArray = new JSONArray();
        for (Category category : categories) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("categoryId", category.getCategoryId());
            jsonObject.put("name", category.getName());
            jsonObject.put("icon", category.getIcon());
            jsonObject.put("type", category.getType());
            jsonObject.put("userId", category.getUserId());
            jsonArray.put(jsonObject);
        }
        return jsonArray;
    }

    // Phương thức chuyển đổi danh sách Transaction thành JSONArray
    private static JSONArray convertTransactionsToJson(List<Transaction> transactions) throws JSONException {
        JSONArray jsonArray = new JSONArray();
        for (Transaction transaction : transactions) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("transactionId", transaction.getTransactionId());
            jsonObject.put("amount", transaction.getAmount());
            jsonObject.put("description", transaction.getDescription());
            jsonObject.put("date", transaction.getDate());
            jsonObject.put("categoryId", transaction.getCategoryId());
            jsonObject.put("userId", transaction.getUserId());
            jsonArray.put(jsonObject);
        }
        return jsonArray;
    }

    // Phương thức chuyển đổi danh sách SpendingAlert thành JSONArray
    private static JSONArray convertAlertsToJson(List<SpendingAlert> alerts) throws JSONException {
        JSONArray jsonArray = new JSONArray();
        for (SpendingAlert alert : alerts) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("alertId", alert.getAlertId());
            jsonObject.put("title", alert.getTitle());
            jsonObject.put("type", alert.getAlertType());
            jsonObject.put("threshold", alert.getThreshold());
            jsonObject.put("priority", alert.getPriority());
            jsonObject.put("active", alert.isActive());
            jsonObject.put("userId", alert.getUserId());
            jsonObject.put("lastNotified", alert.getLastNotified());
            jsonObject.put("notifyTime", alert.getNotifyTime());
            jsonArray.put(jsonObject);
        }
        return jsonArray;
    }
}