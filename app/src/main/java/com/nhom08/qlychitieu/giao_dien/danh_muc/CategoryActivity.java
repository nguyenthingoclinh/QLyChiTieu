package com.nhom08.qlychitieu.giao_dien.danh_muc;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;
import com.nhom08.qlychitieu.MyApplication;
import com.nhom08.qlychitieu.R;
import com.nhom08.qlychitieu.csdl.AppDatabase;
import com.nhom08.qlychitieu.mo_hinh.Category;
import com.nhom08.qlychitieu.truy_van.UserDAO;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CategoryActivity extends AppCompatActivity {
    private static final String TAG = "CategoryActivity";
    private CategoryAdapter categoryAdapter;
    private List<Category> categoryList;
    private AppDatabase database;
    private SharedPreferences sharedPreferences;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private int userId;
    private TabLayout tabLayout;

    private String selectedIconUnicode; // Mặc định là shopping_cart
    private List<String> allIcons; // Danh sách tất cả biểu tượng

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_category);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            v.setPadding(insets.getInsets(WindowInsetsCompat.Type.systemBars()).left,
                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).top,
                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).right,
                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom);
            return insets;
        });

        // Khởi tạo danh sách tất cả biểu tượng
        initializeAllIcons();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        TextView backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
            }
        });

        sharedPreferences = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE);
        database = ((MyApplication) getApplication()).getDatabase();

        tabLayout = findViewById(R.id.tabLayout);
        tabLayout.addTab(tabLayout.newTab().setText(R.string.expense));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.income));

        RecyclerView recyclerViewCategories = findViewById(R.id.recyclerViewCategories);
        recyclerViewCategories.setLayoutManager(new LinearLayoutManager(this));
        categoryList = new ArrayList<>();
        categoryAdapter = new CategoryAdapter(categoryList);
        recyclerViewCategories.setAdapter(categoryAdapter);

        Button btnAddCategory = findViewById(R.id.btnAddCategory);
        btnAddCategory.setOnClickListener(v -> showAddCategoryDialog());

        fetchUserIdAndLoadCategories();
        selectedIconUnicode = getString(R.string.icon_shopping_cart);

        // Thêm sự kiện chuyển tab
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
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
    }

    private void initializeAllIcons() {
        allIcons = new ArrayList<>();
        allIcons.add(getString(R.string.icon_shopping_cart));
        allIcons.add(getString(R.string.icon_fastfood));
        allIcons.add(getString(R.string.icon_directions_car));
        allIcons.add(getString(R.string.icon_movie));
        allIcons.add(getString(R.string.icon_health_and_safety));
        allIcons.add(getString(R.string.icon_school));
        allIcons.add(getString(R.string.icon_sports));
        allIcons.add(getString(R.string.icon_people));
        allIcons.add(getString(R.string.icon_car_repair));
        allIcons.add(getString(R.string.icon_laundry));
        allIcons.add(getString(R.string.icon_account_balance));
        allIcons.add(getString(R.string.icon_card_giftcard));
        allIcons.add(getString(R.string.icon_trending_up));
        allIcons.add(getString(R.string.icon_videogame_asset));
        allIcons.add(getString(R.string.icon_toys));
        allIcons.add(getString(R.string.icon_sports_esports));
        allIcons.add(getString(R.string.icon_headphones));
        allIcons.add(getString(R.string.icon_palette));
        allIcons.add(getString(R.string.icon_camera_alt));
        allIcons.add(getString(R.string.icon_music_note));
        allIcons.add(getString(R.string.icon_sports_volleyball));
        allIcons.add(getString(R.string.icon_rocket_launch));
        allIcons.add(getString(R.string.icon_sports_soccer));
        allIcons.add(getString(R.string.icon_bread_slice));
        allIcons.add(getString(R.string.icon_cake));
        allIcons.add(getString(R.string.icon_local_dining));
        allIcons.add(getString(R.string.icon_icecream));
        allIcons.add(getString(R.string.icon_local_pizza));
        allIcons.add(getString(R.string.icon_lunch_dining));
        allIcons.add(getString(R.string.icon_kebab_dining));
        allIcons.add(getString(R.string.icon_bakery_dining));
        allIcons.add(getString(R.string.icon_ramen_dining));
        allIcons.add(getString(R.string.icon_local_drink));
        allIcons.add(getString(R.string.icon_coffee));
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
                runOnUiThread(() -> updateCategoryList(expenses));
            } catch (Exception e) {
                Log.e(TAG, "Error in loadExpenseCategories: " + e.getMessage(), e);
            }
        });
    }

    private void loadIncomeCategories() {
        executorService.execute(() -> {
            try {
                List<Category> incomes = database.categoryDao().getCategoriesByType(userId, "Income");
                runOnUiThread(() -> updateCategoryList(incomes));
            } catch (Exception e) {
                Log.e(TAG, "Error in loadIncomeCategories: " + e.getMessage(), e);
            }
        });
    }

    private void updateCategoryList(List<Category> newList) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new CategoryDiffCallback(categoryList, newList));
        categoryList.clear();
        categoryList.addAll(newList);
        diffResult.dispatchUpdatesTo(categoryAdapter);
    }

    private void showAddCategoryDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_category, null);
        builder.setView(dialogView);

        EditText etCategoryName = dialogView.findViewById(R.id.etCategoryName);
        Spinner spinnerCategoryType = dialogView.findViewById(R.id.spinnerCategoryType);
        RecyclerView recyclerViewIcons = dialogView.findViewById(R.id.recyclerViewIcons);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        Button btnAdd = dialogView.findViewById(R.id.btnAdd);

        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new String[]{"Chi tiêu", "Thu nhập"});
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategoryType.setAdapter(typeAdapter);

        // Sử dụng IconAdapter trực tiếp với danh sách allIcons
        IconAdapter iconAdapter = new IconAdapter(allIcons);
        recyclerViewIcons.setLayoutManager(new GridLayoutManager(this, 5)); // Hiển thị dạng lưới 5 cột
        recyclerViewIcons.setAdapter(iconAdapter);

        AlertDialog dialog = builder.create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnAdd.setOnClickListener(v -> {
            String name = etCategoryName.getText().toString().trim();
            String type = spinnerCategoryType.getSelectedItem().toString().equals("Chi tiêu") ? "Expense" : "Income";
            String iconUnicode = selectedIconUnicode;

            if (name.isEmpty()) {
                Toast.makeText(CategoryActivity.this, "Vui lòng nhập tên danh mục", Toast.LENGTH_SHORT).show();
                return;
            }

            if (iconUnicode == null || iconUnicode.isEmpty()) {
                Toast.makeText(CategoryActivity.this, "Vui lòng chọn một biểu tượng", Toast.LENGTH_SHORT).show();
                return;
            }

            Category newCategory = new Category(userId, name, type, iconUnicode);
            executorService.execute(() -> {
                try {
                    database.categoryDao().insertCategory(newCategory);
                    runOnUiThread(() -> {
                        Toast.makeText(CategoryActivity.this, "Thêm danh mục thành công", Toast.LENGTH_SHORT).show();
                        if (type.equals("Expense")) {
                            loadExpenseCategories();
                            tabLayout.selectTab(tabLayout.getTabAt(0));
                        } else {
                            loadIncomeCategories();
                            tabLayout.selectTab(tabLayout.getTabAt(1));
                        }
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Error in adding category: " + e.getMessage(), e);
                    runOnUiThread(() -> Toast.makeText(CategoryActivity.this, "Lỗi khi thêm danh mục", Toast.LENGTH_SHORT).show());
                }
            });

            dialog.dismiss();
        });

        dialog.show();
    }

    private class IconAdapter extends RecyclerView.Adapter<IconAdapter.IconViewHolder> {
        private final List<String> icons;
        private int selectedPosition = -1;

        public IconAdapter(List<String> icons) {
            this.icons = icons;
        }

        @NonNull
        @Override
        public IconViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_icon_category, parent, false);
            return new IconViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull IconViewHolder holder, int position) {
            String iconUnicode = icons.get(position);
            holder.iconView.setText(iconUnicode);

            holder.itemView.setSelected(selectedPosition == position);

            holder.itemView.setOnClickListener(v -> {
                int currentPosition = holder.getAdapterPosition();
                if (currentPosition == RecyclerView.NO_POSITION) {
                    return;
                }

                selectedPosition = currentPosition;
                for (int i = 0; i < getItemCount(); i++) {
                    View view = holder.itemView.getRootView().findViewWithTag("icon_" + i);
                    if (view != null) {
                        view.setSelected(i == selectedPosition);
                    }
                }

                selectedIconUnicode = icons.get(currentPosition);
                Toast.makeText(CategoryActivity.this, "Đã chọn biểu tượng: " + selectedIconUnicode, Toast.LENGTH_SHORT).show();
            });

            holder.itemView.setTag("icon_" + position);
        }

        @Override
        public int getItemCount() {
            return icons.size();
        }

        public class IconViewHolder extends RecyclerView.ViewHolder {
            TextView iconView;

            public IconViewHolder(@NonNull View itemView) {
                super(itemView);
                iconView = itemView.findViewById(R.id.iconView);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (!executorService.isShutdown()) {
            executorService.shutdown();
        }
    }

    public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {
        private final List<Category> categories;

        public CategoryAdapter(List<Category> categories) {
            this.categories = categories;
        }

        @NonNull
        @Override
        public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category, parent, false);
            return new CategoryViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
            Category category = categories.get(position);
            holder.categoryName.setText(category.getName());

            String iconValue = category.getIcon();
            Log.d(TAG, "Category: " + category.getName() + ", Icon: " + iconValue);

            if (iconValue != null) {
                holder.categoryIcon.setText(iconValue);
            } else {
                Log.w(TAG, "Icon is not in Unicode format for category: " + category.getName() + ", using default");
                holder.categoryIcon.setText(getString(R.string.icon_shopping_cart));
            }

            int colorResId = Objects.equals(category.getType(), "Income") ? R.color.green : R.color.red;
            holder.categoryIcon.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), colorResId));

            holder.categoryMore.setOnClickListener(v -> {
                PopupMenu popupMenu = new PopupMenu(CategoryActivity.this, holder.categoryMore);
                popupMenu.getMenu().add("Xóa");
                popupMenu.setOnMenuItemClickListener(item -> {
                    if (Objects.equals(item.getTitle(), "Xóa")) {
                        executorService.execute(() -> {
                            try {
                                database.categoryDao().deleteCategory(category);
                                runOnUiThread(() -> {
                                    int currentPosition = holder.getAdapterPosition();
                                    if (currentPosition != RecyclerView.NO_POSITION) {
                                        categories.remove(currentPosition);
                                        notifyItemRemoved(currentPosition);
                                        Toast.makeText(CategoryActivity.this, "Đã xóa danh mục " + category.getName(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                            } catch (Exception e) {
                                Log.e(TAG, "Error in deleting category: " + e.getMessage(), e);
                                runOnUiThread(() -> Toast.makeText(CategoryActivity.this, "Lỗi khi xóa danh mục", Toast.LENGTH_SHORT).show());
                            }
                        });
                    }
                    return true;
                });
                popupMenu.show();
            });
        }

        @Override
        public int getItemCount() {
            return categories.size();
        }

        public class CategoryViewHolder extends RecyclerView.ViewHolder {
            TextView categoryIcon, categoryName, categoryMore;

            public CategoryViewHolder(@NonNull View itemView) {
                super(itemView);
                categoryIcon = itemView.findViewById(R.id.categoryIcon);
                categoryName = itemView.findViewById(R.id.categoryName);
                categoryMore = itemView.findViewById(R.id.categoryMore);
            }
        }
    }

    private static class CategoryDiffCallback extends DiffUtil.Callback {
        private final List<Category> oldList;
        private final List<Category> newList;

        public CategoryDiffCallback(List<Category> oldList, List<Category> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override
        public int getOldListSize() {
            return oldList.size();
        }

        @Override
        public int getNewListSize() {
            return newList.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            return oldList.get(oldItemPosition).getCategoryId() == newList.get(newItemPosition).getCategoryId();
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            Category oldCategory = oldList.get(oldItemPosition);
            Category newCategory = newList.get(newItemPosition);
            return oldCategory.getName().equals(newCategory.getName()) &&
                    Objects.equals(oldCategory.getType(), newCategory.getType()) &&
                    Objects.equals(oldCategory.getIcon(), newCategory.getIcon());
        }
    }
}