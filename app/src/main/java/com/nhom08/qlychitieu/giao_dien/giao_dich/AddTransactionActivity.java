package com.nhom08.qlychitieu.giao_dien.giao_dich;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.tabs.TabLayout;
import com.nhom08.qlychitieu.MyApplication;
import com.nhom08.qlychitieu.R;
import com.nhom08.qlychitieu.csdl.AppDatabase;
import com.nhom08.qlychitieu.mo_hinh.Category;
import com.nhom08.qlychitieu.mo_hinh.Transaction;
import com.nhom08.qlychitieu.truy_van.UserDAO;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AddTransactionActivity extends AppCompatActivity {
    private static final String TAG = "AddTransactionActivity";
    private CategoryAdapter categoryAdapter;
    private List<Category> categoryList;
    private AppDatabase database;
    private SharedPreferences sharedPreferences;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private int userId;
    private LinearLayout layoutAddPhoto;
    private TextView tvAmount, tvDate, tvAttachPhoto, tvNoCategories;
    private EditText etDescription;
    private ImageView ivReceipt;
    private GridView gridCategories;
    private Category selectedCategory;
    private final StringBuilder amountInput = new StringBuilder();
    private long selectedDate;
    private String imagePath = "";
    private ActivityResultLauncher<Intent> pickImageLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_transaction);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            v.setPadding(insets.getInsets(WindowInsetsCompat.Type.systemBars()).left,
                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).top,
                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).right,
                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom);
            return insets;
        });

        // Khởi tạo SharedPreferences
        sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);

        // Lấy database từ MyApplication
        database = ((MyApplication) getApplication()).getDatabase();

        // Thiết lập Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        // Nút Back
        TextView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        // Nút Save
        TextView btnSave = findViewById(R.id.btnSave);
        btnSave.setOnClickListener(v -> saveTransaction());

        // Khởi tạo TabLayout
        TabLayout tabLayout = findViewById(R.id.tabLayout);

        // Khởi tạo GridView và tvNoCategories
        gridCategories = findViewById(R.id.gridCategories);
        tvNoCategories = findViewById(R.id.tvNoCategories);
        categoryList = new ArrayList<>();
        categoryAdapter = new CategoryAdapter(categoryList);
        gridCategories.setAdapter(categoryAdapter);

        // Khởi tạo các thành phần nhập liệu
        LinearLayout layoutDate = findViewById(R.id.layoutDate);
        layoutAddPhoto = findViewById(R.id.layoutAddPhoto);
        tvAmount = findViewById(R.id.tvAmount);
        tvDate = findViewById(R.id.tvDate);
        tvAttachPhoto = findViewById(R.id.tvAttachPhoto);
        etDescription = findViewById(R.id.etDescription);
        ivReceipt = findViewById(R.id.ivReceipt);

        // Khởi tạo ActivityResultLauncher để chọn ảnh
        pickImageLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null && result.getData().getData() != null) {
                imagePath = result.getData().getData().toString();
                ivReceipt.setImageURI(result.getData().getData());
                ivReceipt.setVisibility(View.VISIBLE);
                tvAttachPhoto.setText(getString(R.string.photo_attached));
                // Ẩn TextView chứa icon
                TextView tvIconAddPhoto = layoutAddPhoto.findViewById(R.id.tvIconAddPhoto);
                if (tvIconAddPhoto != null) {
                    tvIconAddPhoto.setVisibility(View.GONE);
                }
            }
        });

        // Xử lý sự kiện chuyển tab
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                etDescription.setText("");
                tvAmount.setText(getString(R.string.number0));
                amountInput.setLength(0);
                selectedCategory = null;
                categoryAdapter.setSelectedCategory(null);
                selectedDate = 0;
                imagePath = "";
                ivReceipt.setVisibility(View.GONE);
                tvAttachPhoto.setText(getString(R.string.attach_photo));
                // Hiện lại TextView chứa icon
                TextView tvIconAddPhoto = layoutAddPhoto.findViewById(R.id.tvIconAddPhoto);
                if (tvIconAddPhoto != null) {
                    tvIconAddPhoto.setVisibility(View.VISIBLE);
                }
                if (tab.getPosition() == 0) {
                    loadExpenseCategories();
                } else {
                    loadIncomeCategories();
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        // Thiết lập bàn phím số
        setupCalculatorButtons();

        // Thiết lập chọn ngày
        layoutDate.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    AddTransactionActivity.this,
                    (view, year1, month1, dayOfMonth) -> {
                        calendar.set(year1, month1, dayOfMonth);
                        selectedDate = calendar.getTimeInMillis();
                        // Chuyển đổi timestamp thành chuỗi ngày tháng dễ đọc
                        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                        String formattedDate = sdf.format(calendar.getTime());
                        tvDate.setText(formattedDate);
                    }, year, month, day);
            datePickerDialog.show();
        });

        // Thiết lập chọn ảnh
        layoutAddPhoto.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            pickImageLauncher.launch(intent);
        });

        // Lấy userId và tải dữ liệu ban đầu (Chi tiêu)
        fetchUserIdAndLoadCategories();
    }

    private void fetchUserIdAndLoadCategories() {
        executorService.execute(() -> {
            try {
                String loggedInEmail = sharedPreferences.getString("loggedInEmail", null);
                if (loggedInEmail == null) {
                    Log.e(TAG, "No logged-in user found");
                    return;
                }

                UserDAO userDao = database.userDao();
                com.nhom08.qlychitieu.mo_hinh.User user = userDao.getUserByEmail(loggedInEmail);
                if (user == null) {
                    Log.e(TAG, "User not found for email: " + loggedInEmail);
                    return;
                }

                userId = user.getUserId();
                Log.d(TAG, "Fetched userId: " + userId);

                runOnUiThread(this::loadExpenseCategories);
            } catch (Exception e) {
                Log.e(TAG, "Error in fetchUserIdAndLoadCategories: " + e.getMessage(), e);
            }
        });
    }

    private void loadExpenseCategories() {
        executorService.execute(() -> {
            try {
                List<Category> expenses = database.categoryDao().getCategoriesByType(userId, "Expense");
                Log.d(TAG, "Loaded expense categories: " + expenses.size());
                runOnUiThread(() -> {
                    categoryList.clear();
                    categoryList.addAll(expenses);
                    selectedCategory = null;
                    categoryAdapter.setSelectedCategory(null);
                    categoryAdapter.notifyDataSetChanged();
                    if (categoryList.isEmpty()) {
                        Log.w(TAG, "No expense categories found for userId: " + userId);
                        tvNoCategories.setVisibility(View.VISIBLE);
                        gridCategories.setVisibility(View.GONE);
                    } else {
                        tvNoCategories.setVisibility(View.GONE);
                        gridCategories.setVisibility(View.VISIBLE);
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error in loadExpenseCategories: " + e.getMessage(), e);
            }
        });
    }

    private void loadIncomeCategories() {
        executorService.execute(() -> {
            try {
                List<Category> incomes = database.categoryDao().getCategoriesByType(userId, "Income");
                Log.d(TAG, "Loaded income categories: " + incomes.size());
                runOnUiThread(() -> {
                    categoryList.clear();
                    categoryList.addAll(incomes);
                    selectedCategory = null;
                    categoryAdapter.setSelectedCategory(null);
                    categoryAdapter.notifyDataSetChanged();
                    if (categoryList.isEmpty()) {
                        Log.w(TAG, "No income categories found for userId: " + userId);
                        tvNoCategories.setVisibility(View.VISIBLE);
                        gridCategories.setVisibility(View.GONE);
                    } else {
                        tvNoCategories.setVisibility(View.GONE);
                        gridCategories.setVisibility(View.VISIBLE);
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error in loadIncomeCategories: " + e.getMessage(), e);
            }
        });
    }

    private void setupCalculatorButtons() {
        // Xử lý các nút số
        int[] numberButtonIds = new int[]{
                R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3,
                R.id.btn4, R.id.btn5, R.id.btn6, R.id.btn7,
                R.id.btn8, R.id.btn9, R.id.btn000, R.id.btnDot
        };
        for (int id : numberButtonIds) {
            Button button = findViewById(id);
            if (button != null) {
                button.setOnClickListener(v -> {
                    String value = button.getText().toString();
                    if (value.equals(getString(R.string.btnDot))) {
                        if (!amountInput.toString().contains(".")) {
                            amountInput.append(".");
                        }
                    } else if (value.equals(getString(R.string.number000))) {
                        amountInput.append("000");
                    } else {
                        amountInput.append(value);
                    }
                    tvAmount.setText(amountInput.toString());
                });
            }
        }

        // Nút cộng và trừ
        AppCompatButton plusButton = findViewById(R.id.btnPlus);
        plusButton.setOnClickListener(v -> {
            String currentText = amountInput.toString();
            if (currentText.isEmpty()) return;
            try {
                Double.parseDouble(currentText);
                amountInput.append("+");
                tvAmount.setText(amountInput.toString());
            } catch (NumberFormatException e) {
                tvAmount.setText(getString(R.string.number0));
                amountInput.setLength(0);
            }
        });

        AppCompatButton minusButton = findViewById(R.id.btnMinus);
        minusButton.setOnClickListener(v -> {
            String currentText = amountInput.toString();
            if (currentText.isEmpty()) return;
            try {
                Double.parseDouble(currentText);
                amountInput.append("-");
                tvAmount.setText(amountInput.toString());
            } catch (NumberFormatException e) {
                tvAmount.setText(getString(R.string.number0));
                amountInput.setLength(0);
            }
        });

        // Nút xóa
        AppCompatButton clearButton = findViewById(R.id.btnClear);
        clearButton.setOnClickListener(v -> {
            amountInput.setLength(0);
            tvAmount.setText(getString(R.string.number0));
        });

        // Nút lưu
        AppCompatButton doneButton = findViewById(R.id.btnDone);
        doneButton.setOnClickListener(v -> saveTransaction());
    }

    private void saveTransaction() {
        if (selectedCategory == null) {
            Toast.makeText(this, "Vui lòng chọn danh mục", Toast.LENGTH_SHORT).show();
            return;
        }

        String amountStr = amountInput.toString();
        if (amountStr.isEmpty() || amountStr.equals("0")) {
            Toast.makeText(this, "Vui lòng nhập số tiền", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount;
        try {
            if (amountStr.contains("+") || amountStr.contains("-")) {
                String[] parts = amountStr.split("[+\\-]");
                double result = Double.parseDouble(parts[0]);
                int i = 1;
                for (int j = 0; j < amountStr.length(); j++) {
                    if (amountStr.charAt(j) == '+') {
                        result += Double.parseDouble(parts[i++]);
                    } else if (amountStr.charAt(j) == '-') {
                        result -= Double.parseDouble(parts[i++]);
                    }
                }
                amount = result;
            } else {
                amount = Double.parseDouble(amountStr);
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Số tiền không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        String description = etDescription.getText().toString().trim();
        long date = selectedDate != 0 ? selectedDate : System.currentTimeMillis();
        Log.d(TAG, "Saving transaction with date: " + date + " (timestamp in millis)"); // Debug log
        Integer accountId = null; // TODO: Implement account selection logic in the future

        Transaction transaction = new Transaction(userId, selectedCategory.getCategoryId(), accountId, amount, date, description, imagePath);
        executorService.execute(() -> {
            try {
                database.transactionDao().insertTransaction(transaction);
                runOnUiThread(() -> {
                    Toast.makeText(AddTransactionActivity.this, "Lưu giao dịch thành công", Toast.LENGTH_SHORT).show();
                    finish();
                });
            } catch (Exception e) {
                Log.e(TAG, "Error in saving transaction: " + e.getMessage(), e);
                runOnUiThread(() -> Toast.makeText(AddTransactionActivity.this, "Lỗi khi lưu giao dịch", Toast.LENGTH_SHORT).show());
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (!executorService.isShutdown()) {
            executorService.shutdown();
        }
    }

    // Adapter cho GridView hiển thị danh sách danh mục
    public class CategoryAdapter extends android.widget.BaseAdapter {
        private final List<Category> categories;
        private Category selectedCategory; // Theo dõi danh mục được chọn

        public CategoryAdapter(List<Category> categories) {
            this.categories = categories;
        }

        // Phương thức để cập nhật danh mục được chọn từ bên ngoài
        public void setSelectedCategory(Category category) {
            this.selectedCategory = category;
            notifyDataSetChanged(); // Cập nhật giao diện
        }

        @Override
        public int getCount() {
            return categories.size();
        }

        @Override
        public Object getItem(int position) {
            return categories.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category_transaction, parent, false);
            }

            Category category = categories.get(position);
            TextView tvCategoryIcon = view.findViewById(R.id.tvCategoryIcon);
            TextView tvCategoryName = view.findViewById(R.id.tvCategoryName);

            // Hiển thị biểu tượng từ category.getIcon() (đã là Unicode)
            String icon = category.getIcon();
            if (icon != null && !icon.isEmpty()) {
                tvCategoryIcon.setText(icon);
                tvCategoryIcon.setTextColor(category.equals(selectedCategory) ? Color.BLACK : Color.GRAY);
            } else {
                tvCategoryIcon.setText(getString(R.string.icon_shopping_cart)); // Default icon
            }

            // Hiển thị tên danh mục
            tvCategoryName.setText(category.getName());
            tvCategoryName.setTextColor(category.equals(selectedCategory) ? Color.BLACK : Color.GRAY);

            // Cập nhật trạng thái được chọn
            view.setSelected(category.equals(selectedCategory));

            // Xử lý sự kiện click
            view.setOnClickListener(v -> {
                this.selectedCategory = category; // Cập nhật danh mục được chọn trong adapter
                AddTransactionActivity.this.selectedCategory = category; // Cập nhật trong activity
                notifyDataSetChanged(); // Cập nhật giao diện
            });

            return view;
        }
    }
}