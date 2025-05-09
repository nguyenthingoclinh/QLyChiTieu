package com.nhom08.qlychitieu.giao_dien.danh_muc;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.PopupMenu;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayout;
import com.nhom08.qlychitieu.MyApplication;
import com.nhom08.qlychitieu.R;
import com.nhom08.qlychitieu.adapter.CategoryAdapter;
import com.nhom08.qlychitieu.csdl.AppDatabase;
import com.nhom08.qlychitieu.databinding.ActivityCategoryBinding;
import com.nhom08.qlychitieu.mo_hinh.Category;
import com.nhom08.qlychitieu.tien_ich.MessageUtils;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CategoryActivity extends AppCompatActivity implements CategoryAdapter.OnCategoryClickListener {
    private static final String TAG = CategoryActivity.class.getSimpleName();

    private ActivityCategoryBinding binding;
    private CategoryAdapter categoryAdapter;
    private MessageUtils messageUtils;
    private AppDatabase database;
    private SharedPreferences sharedPreferences;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private int userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setupStatusBar();
        setupNavigationBar();

        binding = ActivityCategoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initializeComponents();
        setupViews();
        fetchUserAndLoadCategories();
    }

    private void setupStatusBar() {
        Window window = getWindow();

        // Thêm flag để vẽ phía sau status bar
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

        // Xóa flag mặc định
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

        // Thiết lập màu cho status bar cùng màu với toolbar
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.colorPrimary));

    }

    private void setupNavigationBar() {
        Window window = getWindow();
        // Thiết lập màu trắng cho navigation bar
        window.setNavigationBarColor(Color.WHITE);
        // Thiết lập icon navigation bar màu tối vì nền trắng
        View decorView = window.getDecorView();
        int flags = decorView.getSystemUiVisibility();
        flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        decorView.setSystemUiVisibility(flags);
    }

    private void initializeComponents() {
        database = ((MyApplication) getApplication()).getDatabase();
        sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        messageUtils = new MessageUtils(this);
        // Setup RecyclerView và Adapter
        categoryAdapter = new CategoryAdapter(this, this);
        binding.recyclerViewCategories.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerViewCategories.setAdapter(categoryAdapter);
    }

    private void setupViews() {
        setupToolbar();
        setupTabLayout();
        setupAddButton();
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        binding.btnBack.setOnClickListener(v -> finish());
    }

    private void setupTabLayout() {
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(R.string.expense));
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(R.string.income));

        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                loadCategories(tab.getPosition() == 0 ? "Expense" : "Income");
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupAddButton() {
        binding.btnAddCategory.setOnClickListener(v -> showAddCategoryDialog());
    }

    private void fetchUserAndLoadCategories() {
        executorService.execute(() -> {
            try {
                String email = sharedPreferences.getString("loggedInEmail", null);
                if (email == null) {
                    Log.e(TAG, "No logged-in user found");
                    runOnUiThread(() -> messageUtils.showError(R.string.error_user_not_found));
                    return;
                }

                userId = database.userDao().getUserByEmail(email).getUserId();
                runOnUiThread(() -> loadCategories("Expense"));
            } catch (Exception e) {
                Log.e(TAG, "Error fetching user: " + e.getMessage());
                runOnUiThread(() -> messageUtils.showError(R.string.error_loading_user));
            }
        });
    }

    public void loadCategories(String type) {
        executorService.execute(() -> {
            try {
                List<Category> categories = database.categoryDao()
                        .getCategoriesByType(userId, type);
                runOnUiThread(() -> updateCategoryList(categories));
            } catch (Exception e) {
                Log.e(TAG, "Error loading categories: " + e.getMessage());
                runOnUiThread(() -> messageUtils.showError(R.string.error_loading_categories));
            }
        });
    }

    private void updateCategoryList(List<Category> categories) {
        categories.forEach(category -> {
            // Chuyển đổi type từ tiếng Anh sang tiếng Việt khi hiển thị
            if ("Expense".equals(category.getType())) {
                category.setDisplayType("Chi tiêu");
            } else if ("Income".equals(category.getType())) {
                category.setDisplayType("Thu nhập");
            }
        });
        categoryAdapter.updateCategories(categories);
    }

    @Override
    public void onCategoryClick(Category category) {
        showEditCategoryDialog(category);
    }

    @Override
    public void onCategoryMoreClick(Category category, View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenu().add(Menu.NONE, 1, Menu.NONE, R.string.edit);
        popup.getMenu().add(Menu.NONE, 2, Menu.NONE, R.string.delete);

        popup.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 1) {
                showEditCategoryDialog(category);
                return true;
            } else if (item.getItemId() == 2) {
                showDeleteCategoryDialog(category);
                return true;
            }
            return false;
        });

        popup.show();
    }

    private void showAddCategoryDialog() {
        String type = binding.tabLayout.getSelectedTabPosition() == 0 ? "Expense" : "Income";
        AddCategoryDialog dialog = new AddCategoryDialog(
                this,
                userId,
                database,
                () -> loadCategories(type)  // Callback khi thêm xong
        );
        dialog.show(getSupportFragmentManager(), "AddCategoryDialog");
    }

    private void showEditCategoryDialog(Category category) {
        EditCategoryDialog dialog = new EditCategoryDialog(
                this,
                category,
                database,
                () -> loadCategories(category.getType())  // Callback khi sửa xong
        );
        dialog.show(getSupportFragmentManager(), "EditCategoryDialog");
    }

    private void showDeleteCategoryDialog(Category category) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete_category)
                .setMessage(R.string.delete_category_confirmation)
                .setPositiveButton(R.string.delete, (dialog, which) -> deleteCategory(category))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void deleteCategory(Category category) {
        executorService.execute(() -> {
            try {
                database.categoryDao().deleteCategory(category);
                runOnUiThread(() -> {
                    messageUtils.showSuccess(R.string.category_deleted);
                    loadCategories(category.getType());
                });
            } catch (Exception e) {
                Log.e(TAG, "Error deleting category: " + e.getMessage());
                runOnUiThread(() -> messageUtils.showError(R.string.error_deleting_category));
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (!executorService.isShutdown()) {
            executorService.shutdown();
        }
        binding = null;
    }
}