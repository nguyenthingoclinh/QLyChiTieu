package com.nhom08.qlychitieu.giao_dien.danh_muc;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.PopupMenu;

import androidx.annotation.StringRes;
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
import com.nhom08.qlychitieu.tien_ich.Constants;
import com.nhom08.qlychitieu.tien_ich.MessageUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

public class CategoryActivity extends AppCompatActivity implements CategoryAdapter.OnCategoryClickListener {
    private static final String TAG = CategoryActivity.class.getSimpleName();
    private ActivityCategoryBinding binding;
    private CategoryAdapter categoryAdapter;
    private MessageUtils messageUtils;
    private AppDatabase database;
    private ExecutorService executorService;
    private int userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupWindowDecorations();

        binding = ActivityCategoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initGlobals();
        setupViews();
        loadCategories(Constants.CATEGORY_TYPE_EXPENSE); // Load default tab
    }

    private void setupWindowDecorations() {
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.colorPrimary));
        window.setNavigationBarColor(Color.WHITE);
        int flags = window.getDecorView().getSystemUiVisibility() | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        window.getDecorView().setSystemUiVisibility(flags);
    }

    private void initGlobals() {
        MyApplication app = MyApplication.getInstance();
        database = app.getDatabase();
        executorService = app.getExecutorService();
        messageUtils = new MessageUtils(this);
        userId = app.getCurrentUserId();

        if (userId == -1) {
            messageUtils.showError(R.string.error_user_not_found);
            finish();
        }

        categoryAdapter = new CategoryAdapter(this, new ArrayList<>(), this);
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
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayShowTitleEnabled(false);
        binding.btnBack.setOnClickListener(v -> finish());
    }

    private void setupTabLayout() {
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(R.string.expense));
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(R.string.income));
        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                loadCategories(tab.getPosition() == 0 ? Constants.CATEGORY_TYPE_EXPENSE : Constants.CATEGORY_TYPE_INCOME);
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupAddButton() {
        binding.btnAddCategory.setOnClickListener(v -> showAddCategoryDialog());
    }

    private void loadCategories(String type) {
        executorService.execute(() -> {
            try {
                List<Category> categories = database.categoryDao().getCategoriesByType(userId, type);
                runOnUiThread(() -> updateCategoryList(categories));
            } catch (Exception e) {
                handleError("Error loading categories: " + e.getMessage(), R.string.error_loading_categories);
            }
        });
    }

    private void handleError(String logMessage, @StringRes int userMessage) {
        Log.e(TAG, logMessage);
        runOnUiThread(() -> messageUtils.showError(userMessage));
    }

    private void updateCategoryList(List<Category> categories) {
        for (Category category : categories) {
            // Hiển thị tiếng Việt cho type
            category.setDisplayType("Expense".equals(category.getType()) ? "Chi tiêu" : "Thu nhập");
        }
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
        String type = binding.tabLayout.getSelectedTabPosition() == 0
                ? Constants.CATEGORY_TYPE_EXPENSE
                : Constants.CATEGORY_TYPE_INCOME;
        AddCategoryDialog dialog = new AddCategoryDialog(
                this,
                userId,
                type,
                database,
                () -> loadCategories(type)
        );
        dialog.show(getSupportFragmentManager(), "AddCategoryDialog");
    }

    private void showEditCategoryDialog(Category category) {
        EditCategoryDialog dialog = new EditCategoryDialog(
                this,
                category,
                database,
                () -> loadCategories(category.getType())
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
        binding = null;
    }
}