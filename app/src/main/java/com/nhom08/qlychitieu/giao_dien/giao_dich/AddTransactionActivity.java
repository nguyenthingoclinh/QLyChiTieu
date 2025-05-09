package com.nhom08.qlychitieu.giao_dien.giao_dich;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.BaseAdapter;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.tabs.TabLayout;
import com.nhom08.qlychitieu.MyApplication;
import com.nhom08.qlychitieu.R;
import com.nhom08.qlychitieu.csdl.AppDatabase;
import com.nhom08.qlychitieu.databinding.ActivityAddTransactionBinding;
import com.nhom08.qlychitieu.databinding.ItemCategoryTransactionBinding;
import com.nhom08.qlychitieu.mo_hinh.Category;
import com.nhom08.qlychitieu.mo_hinh.Transaction;
import com.nhom08.qlychitieu.tien_ich.CalculatorHandler;
import com.nhom08.qlychitieu.tien_ich.ImageUtils;
import com.nhom08.qlychitieu.tien_ich.MessageUtils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AddTransactionActivity extends AppCompatActivity {
    private static final String TAG = AddTransactionActivity.class.getSimpleName();

    private ActivityAddTransactionBinding binding;
    private ImageUtils imageUtils;
    private MessageUtils messageUtils;
    private CalculatorHandler calculatorHandler;
    private CategoryAdapter categoryAdapter;
    private AppDatabase database;
    private SharedPreferences sharedPreferences;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private Category selectedCategory;
    private long selectedDate;
    private String imagePath = "";
    private int userId;
    private ActivityResultLauncher<Intent> pickImageLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        // Set up status bar color
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.colorPrimary));

        // Set dark icons on status bar
        View decorView = window.getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        // Initialize ViewBinding
        binding = ActivityAddTransactionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            v.setPadding(
                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).left,
                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).top,
                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).right,
                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom);
            return insets;
        });

        // Initialize utils
        imageUtils = new ImageUtils(this);
        messageUtils = new MessageUtils(this);
        calculatorHandler = new CalculatorHandler();

        // Initialize database and preferences
        database = ((MyApplication) getApplication()).getDatabase();
        sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);

        setupViews();
        setupImagePicker();
        setupCalculator();
        setupDatePicker();
        setupCategoryAdapter();
        fetchUserAndLoadCategories();
    }

    private void setupViews() {
        // Setup toolbar
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        // Setup click listeners
        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnSave.setOnClickListener(v -> saveTransaction());
    }

    private void setupImagePicker() {
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        handleImageResult(result.getData().getData());
                    }
                }
        );

        binding.layoutAddPhoto.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            pickImageLauncher.launch(intent);
        });
    }

    private void setupDatePicker() {
        binding.layoutDate.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    this,
                    (view, year, month, dayOfMonth) -> {
                        calendar.set(year, month, dayOfMonth);
                        selectedDate = calendar.getTimeInMillis();
                        updateDateDisplay(calendar.getTime());
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.show();
        });
    }

    private void setupCalculator() {
        setupNumberButtons();
        setupOperatorButtons();
        setupActionButtons();
    }

    private void setupNumberButtons() {
        int[] numberButtonIds = {
                R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3,
                R.id.btn4, R.id.btn5, R.id.btn6, R.id.btn7,
                R.id.btn8, R.id.btn9, R.id.btn000, R.id.btnDot
        };

        for (int id : numberButtonIds) {
            binding.getRoot().findViewById(id).setOnClickListener(v -> {
                String value = ((android.widget.Button) v).getText().toString();
                handleNumberInput(value);
            });
        }
    }

    private void handleNumberInput(String value) {
        switch (value) {
            case ".":
                calculatorHandler.appendDot();
                break;
            case "000":
                calculatorHandler.append000();
                break;
            default:
                calculatorHandler.appendNumber(value);
                break;
        }
        updateAmountDisplay();
    }

    private void setupOperatorButtons() {
        binding.btnPlus.setOnClickListener(v ->
                handleOperator("+"));
        binding.btnMinus.setOnClickListener(v ->
                handleOperator("-"));
    }

    private void handleOperator(String operator) {
        calculatorHandler.appendOperator(operator);
        updateAmountDisplay();
    }

    private void setupActionButtons() {
        binding.btnClear.setOnClickListener(v -> {
            calculatorHandler.clear();
            updateAmountDisplay();
        });
        binding.btnDone.setOnClickListener(v -> saveTransaction());
    }

    private void setupCategoryAdapter() {
        // Khởi tạo adapter với danh sách rỗng
        categoryAdapter = new CategoryAdapter(new ArrayList<>());
        binding.gridCategories.setAdapter(categoryAdapter);

        // Setup tab listener để chuyển đổi giữa Chi tiêu và Thu nhập
        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                // Reset tất cả các input khi chuyển tab
                resetInputs();
                // Load danh mục tương ứng với tab được chọn
                loadCategories(tab.getPosition() == 0 ? "Expense" : "Income");
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                // Không cần xử lý
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                // Không cần xử lý
            }
        });

        // Set click listener cho GridView
        binding.gridCategories.setOnItemClickListener((parent, view, position, id) -> {
            Category category = categoryAdapter.getItem(position);
            selectedCategory = category;
            categoryAdapter.setSelectedCategory(category);
        });
    }

    private void updateAmountDisplay() {
        binding.tvAmount.setText(calculatorHandler.getCurrentInput());
    }

    private void handleImageResult(Uri imageUri) {
        if (imageUri == null) return;

        String savedPath = imageUtils.saveImage(imageUri);
        if (savedPath != null) {
            imagePath = savedPath;
            binding.ivReceipt.setImageURI(Uri.fromFile(new File(savedPath)));
            binding.ivReceipt.setVisibility(View.VISIBLE);
            binding.tvAttachPhoto.setText(R.string.photo_attached);
            hideAddPhotoIcon();
        } else {
            messageUtils.showError(R.string.error_saving_image);
        }
    }

    private void hideAddPhotoIcon() {
        View iconView = binding.layoutAddPhoto.findViewById(R.id.tvIconAddPhoto);
        if (iconView != null) {
            iconView.setVisibility(View.GONE);
        }
    }

    private void showAddPhotoIcon() {
        View iconView = binding.layoutAddPhoto.findViewById(R.id.tvIconAddPhoto);
        if (iconView != null) {
            iconView.setVisibility(View.VISIBLE);
        }
    }

    private void updateDateDisplay(Date date) {
        String formattedDate = new SimpleDateFormat("dd/MM/yyyy",
                Locale.getDefault()).format(date);
        binding.tvDate.setText(formattedDate);
    }

    private void fetchUserAndLoadCategories() {
        executorService.execute(() -> {
            try {
                String email = sharedPreferences.getString("loggedInEmail", null);
                if (email == null) {
                    Log.e(TAG, "No logged-in user found");
                    return;
                }

                userId = database.userDao().getUserByEmail(email).getUserId();
                runOnUiThread(() -> loadCategories("Expense"));
            } catch (Exception e) {
                Log.e(TAG, "Error fetching user: " + e.getMessage());
            }
        });
    }

    private void loadCategories(String type) {
        executorService.execute(() -> {
            try {
                List<Category> categories = database.categoryDao()
                        .getCategoriesByType(userId, type);
                runOnUiThread(() -> updateCategoryList(categories));
            } catch (Exception e) {
                Log.e(TAG, "Error loading categories: " + e.getMessage());
                runOnUiThread(() ->
                        messageUtils.showError(R.string.error_loading_categories));
            }
        });
    }

    private void updateCategoryList(List<Category> categories) {
        categoryAdapter.updateCategories(categories);

        if (categories.isEmpty()) {
            binding.tvNoCategories.setVisibility(View.VISIBLE);
            binding.gridCategories.setVisibility(View.GONE);
        } else {
            binding.tvNoCategories.setVisibility(View.GONE);
            binding.gridCategories.setVisibility(View.VISIBLE);
        }
    }

    private void saveTransaction() {
        if (!validateInputs()) return;

        try {
            double amount = calculatorHandler.calculate();
            if (amount == 0) {
                messageUtils.showError(R.string.error_invalid_amount);
                return;
            }

            // Adjust amount based on transaction type
            if (selectedCategory.getType().equals("Expense")) {
                amount = -Math.abs(amount);
            }

            final Transaction transaction = new Transaction(
                    userId,
                    selectedCategory.getCategoryId(),
                    null, // accountId
                    amount,
                    selectedDate != 0 ? selectedDate : System.currentTimeMillis(),
                    binding.etDescription.getText().toString().trim(),
                    imagePath
            );

            saveTransactionToDb(transaction);

        } catch (NumberFormatException e) {
            messageUtils.showError(R.string.error_invalid_amount);
        }
    }

    private boolean validateInputs() {
        if (selectedCategory == null) {
            messageUtils.showError(R.string.error_no_category);
            return false;
        }

        if (calculatorHandler.getCurrentInput().isEmpty()) {
            messageUtils.showError(R.string.error_no_amount);
            return false;
        }

        return true;
    }

    private void saveTransactionToDb(Transaction transaction) {
        executorService.execute(() -> {
            try {
                database.transactionDao().insertTransaction(transaction);
                runOnUiThread(() -> {
                    messageUtils.showSuccess(R.string.success_save_transaction);
                    finish();
                });
            } catch (Exception e) {
                Log.e(TAG, "Error saving transaction: " + e.getMessage());
                runOnUiThread(() ->
                        messageUtils.showError(R.string.error_save_transaction));
            }
        });
    }

    private void resetInputs() {
        binding.etDescription.setText("");
        calculatorHandler.clear();
        updateAmountDisplay();
        selectedCategory = null;
        categoryAdapter.setSelectedCategory(null);
        selectedDate = 0;
        imagePath = "";
        binding.ivReceipt.setVisibility(View.GONE);
        binding.tvAttachPhoto.setText(R.string.attach_photo);
        showAddPhotoIcon();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (!executorService.isShutdown()) {
            executorService.shutdown();
        }
        binding = null;
    }

    // CategoryAdapter inner class
    private class CategoryAdapter extends BaseAdapter {
        private List<Category> categories;
        private Category selectedCategory;

        public CategoryAdapter(List<Category> categories) {
            this.categories = categories;
        }

        public void updateCategories(List<Category> newCategories) {
            this.categories = new ArrayList<>(newCategories);
            notifyDataSetChanged();
        }

        public void setSelectedCategory(Category category) {
            this.selectedCategory = category;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return categories.size();
        }

        @Override
        public Category getItem(int position) {
            return categories.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ItemCategoryTransactionBinding itemBinding;

            if (convertView == null) {
                itemBinding = ItemCategoryTransactionBinding.inflate(
                        LayoutInflater.from(parent.getContext()),
                        parent,
                        false
                );
                convertView = itemBinding.getRoot();
                convertView.setTag(itemBinding);
            } else {
                itemBinding = (ItemCategoryTransactionBinding) convertView.getTag();
            }

            Category category = getItem(position);
            boolean isSelected = category.equals(selectedCategory);

            itemBinding.tvCategoryIcon.setText(category.getIcon());
            itemBinding.tvCategoryIcon.setTextColor(
                    isSelected ? Color.BLACK : Color.GRAY);

            itemBinding.tvCategoryName.setText(category.getName());
            itemBinding.tvCategoryName.setTextColor(
                    isSelected ? Color.BLACK : Color.GRAY);

            convertView.setOnClickListener(v -> {
                this.selectedCategory = category;
                AddTransactionActivity.this.selectedCategory = category;
                notifyDataSetChanged();
            });

            return convertView;
        }
    }
}