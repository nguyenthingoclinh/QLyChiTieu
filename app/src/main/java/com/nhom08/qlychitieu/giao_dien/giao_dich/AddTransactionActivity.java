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

/**
 * AddTransactionActivity cho phép người dùng thêm giao dịch mới.
 * - Hiển thị tab Chi tiêu và Thu nhập.
 * - Người dùng có thể nhập số tiền, chọn danh mục, ngày, mô tả và đính kèm ảnh.
 * - Lưu giao dịch vào database với số tiền âm nếu là Chi tiêu và dương nếu là Thu nhập.
 */
public class AddTransactionActivity extends AppCompatActivity {
    private static final String TAG = "AddTransactionActivity"; // Tag để ghi log lỗi hoặc thông tin debug
    private CategoryAdapter categoryAdapter; // Adapter cho GridView hiển thị danh mục
    private List<Category> categoryList; // Danh sách danh mục
    private AppDatabase database; // Cơ sở dữ liệu Room để truy xuất và lưu dữ liệu
    private SharedPreferences sharedPreferences; // SharedPreferences để lưu trữ thông tin người dùng
    private final ExecutorService executorService = Executors.newSingleThreadExecutor(); // Thread pool cho tác vụ bất đồng bộ
    private int userId; // ID của người dùng hiện tại
    private LinearLayout layoutAddPhoto; // Layout để đính kèm ảnh
    private TextView tvAmount, tvDate, tvAttachPhoto, tvNoCategories; // Các TextView hiển thị thông tin
    private EditText etDescription; // EditText để nhập mô tả giao dịch
    private ImageView ivReceipt; // ImageView để hiển thị ảnh biên lai
    private GridView gridCategories; // GridView hiển thị danh sách danh mục
    private Category selectedCategory; // Danh mục được chọn
    private final StringBuilder amountInput = new StringBuilder(); // Chuỗi để nhập số tiền
    private long selectedDate; // Ngày được chọn (timestamp)
    private String imagePath = ""; // Đường dẫn ảnh biên lai
    private ActivityResultLauncher<Intent> pickImageLauncher; // Launcher để chọn ảnh từ thư viện

    /**
     * Khởi tạo giao diện và các thành phần của Activity.
     * - Thiết lập Toolbar, TabLayout, GridView, và các nút.
     * - Gán sự kiện click để chọn ngày, đính kèm ảnh, và lưu giao dịch.
     * - Tải danh mục ban đầu (Chi tiêu).
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this); // Kích hoạt chế độ toàn màn hình
        setContentView(R.layout.activity_add_transaction);

        // Xử lý padding cho system bars
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

        // Nút Back để quay lại
        TextView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        // Nút Save để lưu giao dịch
        TextView btnSave = findViewById(R.id.btnSave);
        btnSave.setOnClickListener(v -> saveTransaction());

        // Khởi tạo TabLayout để chọn giữa Chi tiêu và Thu nhập
        TabLayout tabLayout = findViewById(R.id.tabLayout);

        // Khởi tạo GridView và TextView hiển thị thông báo khi không có danh mục
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

        // Khởi tạo ActivityResultLauncher để chọn ảnh từ thư viện
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

        // Xử lý sự kiện chuyển tab giữa Chi tiêu và Thu nhập
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                // Xóa các trường nhập liệu khi chuyển tab
                etDescription.setText("");
                tvAmount.setText(getString(R.string.number0));
                amountInput.setLength(0);
                selectedCategory = null;
                categoryAdapter.setSelectedCategory(null);
                selectedDate = 0;
                imagePath = "";
                ivReceipt.setVisibility(View.GONE);
                tvAttachPhoto.setText(getString(R.string.attach_photo));
                TextView tvIconAddPhoto = layoutAddPhoto.findViewById(R.id.tvIconAddPhoto);
                if (tvIconAddPhoto != null) {
                    tvIconAddPhoto.setVisibility(View.VISIBLE);
                }
                // Tải danh mục tương ứng với tab
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

        // Thiết lập bàn phím số để nhập số tiền
        setupCalculatorButtons();

        // Thiết lập chọn ngày giao dịch
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
                        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                        String formattedDate = sdf.format(calendar.getTime());
                        tvDate.setText(formattedDate);
                    }, year, month, day);
            datePickerDialog.show();
        });

        // Thiết lập chọn ảnh biên lai
        layoutAddPhoto.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            pickImageLauncher.launch(intent);
        });

        // Lấy userId và tải danh mục ban đầu (Chi tiêu)
        fetchUserIdAndLoadCategories();
    }

    /**
     * Lấy userId từ SharedPreferences và tải danh mục ban đầu (Chi tiêu).
     * - Thực hiện bất đồng bộ bằng ExecutorService.
     */
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

    /**
     * Tải danh mục Chi tiêu từ database.
     * - Cập nhật GridView và hiển thị thông báo nếu không có danh mục.
     */
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

    /**
     * Tải danh mục Thu nhập từ database.
     * - Cập nhật GridView và hiển thị thông báo nếu không có danh mục.
     */
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

    /**
     * Thiết lập các nút trên bàn phím số.
     * - Xử lý nhập số, phép tính cộng/trừ, xóa và lưu giao dịch.
     */
    private void setupCalculatorButtons() {
        // Xử lý các nút số (0-9, 000, dấu chấm)
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

        // Nút cộng
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

        // Nút trừ
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

        // Nút lưu giao dịch
        AppCompatButton doneButton = findViewById(R.id.btnDone);
        doneButton.setOnClickListener(v -> saveTransaction());
    }

    /**
     * Lưu giao dịch vào database.
     * - Kiểm tra dữ liệu đầu vào (danh mục, số tiền).
     * - Điều chỉnh số tiền: âm nếu là Chi tiêu, dương nếu là Thu nhập.
     * - Lưu giao dịch bất đồng bộ và thông báo kết quả.
     */
    private void saveTransaction() {
        // Kiểm tra danh mục đã được chọn chưa
        if (selectedCategory == null) {
            Toast.makeText(this, "Vui lòng chọn danh mục", Toast.LENGTH_SHORT).show();
            return;
        }

        // Kiểm tra số tiền có hợp lệ không
        String amountStr = amountInput.toString();
        if (amountStr.isEmpty() || amountStr.equals("0")) {
            Toast.makeText(this, "Vui lòng nhập số tiền", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount;
        try {
            // Xử lý phép tính nếu có dấu + hoặc -
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

            // Điều chỉnh số tiền dựa trên loại danh mục
            // Nếu là Chi tiêu (Expense), số tiền sẽ là âm
            // Nếu là Thu nhập (Income), số tiền giữ nguyên (dương)
            if (selectedCategory.getType().equals("Expense")) {
                amount = -Math.abs(amount); // Đảm bảo số tiền là âm
                Log.d(TAG, "Adjusted amount for Expense: " + amount);
            } else if (selectedCategory.getType().equals("Income")) {
                amount = Math.abs(amount); // Đảm bảo số tiền là dương
                Log.d(TAG, "Adjusted amount for Income: " + amount);
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Số tiền không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        // Lấy các thông tin khác của giao dịch
        String description = etDescription.getText().toString().trim();
        long date = selectedDate != 0 ? selectedDate : System.currentTimeMillis();
        Log.d(TAG, "Saving transaction with date: " + date + " (timestamp in millis)");
        Integer accountId = null; // TODO: Implement account selection logic in the future

        // Tạo đối tượng Transaction và lưu vào database
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

    /**
     * Hủy ExecutorService khi Activity bị hủy để tránh rò rỉ bộ nhớ.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (!executorService.isShutdown()) {
            executorService.shutdown();
        }
    }

    /**
     * Adapter cho GridView hiển thị danh sách danh mục.
     */
    public class CategoryAdapter extends android.widget.BaseAdapter {
        private final List<Category> categories; // Danh sách danh mục
        private Category selectedCategory; // Danh mục được chọn

        /**
         * Constructor cho CategoryAdapter.
         *
         * @param categories Danh sách danh mục
         */
        public CategoryAdapter(List<Category> categories) {
            this.categories = categories;
        }

        /**
         * Cập nhật danh mục được chọn và làm mới giao diện.
         *
         * @param category Danh mục được chọn
         */
        public void setSelectedCategory(Category category) {
            this.selectedCategory = category;
            notifyDataSetChanged();
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

        /**
         * Hiển thị mỗi danh mục trong GridView.
         * - Hiển thị biểu tượng và tên danh mục.
         * - Đổi màu chữ khi danh mục được chọn.
         *
         * @param position Vị trí của danh mục
         * @param convertView View tái sử dụng
         * @param parent ViewGroup cha
         * @return View của danh mục
         */
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
                tvCategoryIcon.setText(getString(R.string.icon_shopping_cart));
            }

            // Hiển thị tên danh mục
            tvCategoryName.setText(category.getName());
            tvCategoryName.setTextColor(category.equals(selectedCategory) ? Color.BLACK : Color.GRAY);

            // Cập nhật trạng thái được chọn
            view.setSelected(category.equals(selectedCategory));

            // Xử lý sự kiện click để chọn danh mục
            view.setOnClickListener(v -> {
                this.selectedCategory = category;
                AddTransactionActivity.this.selectedCategory = category;
                notifyDataSetChanged();
            });

            return view;
        }
    }
}