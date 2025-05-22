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

/**
 * Màn hình quản lý danh mục (Category) cho phép người dùng xem, thêm, sửa và xóa
 * danh mục chi tiêu và thu nhập.
 */
public class CategoryActivity extends AppCompatActivity implements CategoryAdapter.OnCategoryClickListener {
    // Tag để sử dụng trong log
    private static final String TAG = CategoryActivity.class.getSimpleName();

    // View binding để truy cập các thành phần giao diện
    private ActivityCategoryBinding binding;

    // Adapter để hiển thị danh sách danh mục
    private CategoryAdapter categoryAdapter;

    // Tiện ích hiển thị thông báo
    private MessageUtils messageUtils;

    // Đối tượng truy cập cơ sở dữ liệu
    private AppDatabase database;

    // Đối tượng xử lý tác vụ bất đồng bộ
    private ExecutorService executorService;

    // ID người dùng hiện tại
    private int userId;

    /**
     * Được gọi khi activity được tạo lần đầu
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        thietLapDecorationCuaSo();

        // Khởi tạo view binding
        binding = ActivityCategoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Khởi tạo các biến toàn cục
        khoiTaoBienToanCuc();

        // Thiết lập các thành phần giao diện
        thietLapGiaoDien();

        // Tải danh mục chi tiêu mặc định
        taiDanhMuc(Constants.CATEGORY_TYPE_EXPENSE); // Tải tab mặc định
    }

    /**
     * Thiết lập cửa sổ (thanh trạng thái, thanh điều hướng, ...)
     */
    private void thietLapDecorationCuaSo() {
        Window window = getWindow();
        // Thêm flag để vẽ background cho thanh trạng thái
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        // Xóa flag để thanh trạng thái không trong suốt
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        // Đặt màu cho thanh trạng thái
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.colorPrimary));
        // Đặt màu cho thanh điều hướng
        window.setNavigationBarColor(Color.WHITE);
        // Thiết lập icon thanh điều hướng màu tối trên nền sáng
        int flags = window.getDecorView().getSystemUiVisibility() | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        window.getDecorView().setSystemUiVisibility(flags);
    }

    /**
     * Khởi tạo các biến toàn cục cần thiết cho activity
     */
    private void khoiTaoBienToanCuc() {
        // Lấy các thực thể từ MyApplication
        MyApplication app = MyApplication.getInstance();
        database = app.getDatabase();
        executorService = app.getExecutorService();
        messageUtils = new MessageUtils(this);
        userId = app.getCurrentUserId();

        // Kiểm tra người dùng đã đăng nhập chưa
        if (userId == -1) {
            messageUtils.showError(R.string.error_user_not_found);
            finish();
        }

        // Khởi tạo adapter với danh sách trống
        categoryAdapter = new CategoryAdapter(this, new ArrayList<>(), this);
        binding.recyclerViewCategories.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerViewCategories.setAdapter(categoryAdapter);
    }

    /**
     * Thiết lập các thành phần giao diện
     */
    private void thietLapGiaoDien() {
        thietLapThanhCongCu();
        thietLapTabLayout();
        thietLapNutThem();
    }

    /**
     * Thiết lập thanh công cụ (toolbar)
     */
    private void thietLapThanhCongCu() {
        setSupportActionBar(binding.toolbar);
        // Ẩn tiêu đề mặc định của ActionBar
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayShowTitleEnabled(false);
        // Thiết lập sự kiện nút quay lại
        binding.btnBack.setOnClickListener(v -> finish());
    }

    /**
     * Thiết lập TabLayout với các tab Chi tiêu và Thu nhập
     */
    private void thietLapTabLayout() {
        // Thêm tab Chi tiêu
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(R.string.expense));
        // Thêm tab Thu nhập
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(R.string.income));
        // Thiết lập sự kiện khi chuyển tab
        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                // Tải danh mục tương ứng với tab được chọn
                taiDanhMuc(tab.getPosition() == 0 ? Constants.CATEGORY_TYPE_EXPENSE : Constants.CATEGORY_TYPE_INCOME);
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    /**
     * Thiết lập nút thêm danh mục
     */
    private void thietLapNutThem() {
        // Thiết lập sự kiện khi nhấn nút thêm
        binding.btnAddCategory.setOnClickListener(v -> hienThiDialogThemDanhMuc());
    }

    /**
     * Tải danh sách danh mục theo loại từ cơ sở dữ liệu
     *
     * @param type Loại danh mục (Chi tiêu hoặc Thu nhập)
     */
    private void taiDanhMuc(String type) {
        executorService.execute(() -> {
            try {
                // Truy vấn danh sách danh mục theo loại và người dùng
                List<Category> categories = database.categoryDao().getCategoriesByType(userId, type);
                // Cập nhật giao diện trên luồng chính
                runOnUiThread(() -> capNhatDanhSachDanhMuc(categories));
            } catch (Exception e) {
                // Xử lý lỗi nếu có
                xuLyLoi("Lỗi khi tải danh mục: " + e.getMessage(), R.string.error_loading_categories);
            }
        });
    }

    /**
     * Xử lý lỗi và hiển thị thông báo cho người dùng
     *
     * @param logMessage Thông báo ghi vào log
     * @param userMessage ID của chuỗi thông báo lỗi trong resources
     */
    private void xuLyLoi(String logMessage, @StringRes int userMessage) {
        // Ghi log lỗi
        Log.e(TAG, logMessage);
        // Hiển thị thông báo lỗi cho người dùng trên luồng UI
        runOnUiThread(() -> messageUtils.showError(userMessage));
    }

    /**
     * Cập nhật danh sách danh mục trên giao diện
     *
     * @param categories Danh sách danh mục cần hiển thị
     */
    private void capNhatDanhSachDanhMuc(List<Category> categories) {
        // Chuyển đổi loại danh mục sang tiếng Việt để hiển thị
        for (Category category : categories) {
            // Hiển thị tiếng Việt cho type
            category.setDisplayType("Expense".equals(category.getType()) ? "Chi tiêu" : "Thu nhập");
        }
        // Cập nhật adapter với danh sách mới
        categoryAdapter.updateCategories(categories);
    }

    /**
     * Xử lý sự kiện khi người dùng nhấn vào một danh mục
     *
     * @param category Danh mục được nhấn
     */
    @Override
    public void onCategoryClick(Category category) {
        // Hiển thị dialog chỉnh sửa danh mục
        hienThiDialogSuaDanhMuc(category);
    }

    /**
     * Xử lý sự kiện khi người dùng nhấn vào nút "Thêm" trong danh mục
     *
     * @param category Danh mục được chọn
     * @param anchor View làm điểm neo cho popup menu
     */
    @Override
    public void onCategoryMoreClick(Category category, View anchor) {
        // Tạo popup menu
        PopupMenu popup = new PopupMenu(this, anchor);
        // Thêm các mục menu
        popup.getMenu().add(Menu.NONE, 1, Menu.NONE, R.string.edit);
        popup.getMenu().add(Menu.NONE, 2, Menu.NONE, R.string.delete);
        // Thiết lập xử lý sự kiện khi chọn mục menu
        popup.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 1) {
                // Hiển thị dialog chỉnh sửa nếu nhấn "Sửa"
                hienThiDialogSuaDanhMuc(category);
                return true;
            } else if (item.getItemId() == 2) {
                // Hiển thị dialog xác nhận xóa nếu nhấn "Xóa"
                hienThiDialogXoaDanhMuc(category);
                return true;
            }
            return false;
        });
        // Hiển thị popup menu
        popup.show();
    }

    /**
     * Hiển thị dialog thêm danh mục mới
     */
    private void hienThiDialogThemDanhMuc() {
        // Xác định loại danh mục dựa vào tab đang chọn
        String type = binding.tabLayout.getSelectedTabPosition() == 0
                ? Constants.CATEGORY_TYPE_EXPENSE
                : Constants.CATEGORY_TYPE_INCOME;
        // Tạo và hiển thị dialog thêm danh mục
        AddCategoryDialog dialog = new AddCategoryDialog(
                this,
                userId,
                type,
                database,
                () -> taiDanhMuc(type) // Callback để tải lại danh sách sau khi thêm
        );
        dialog.show(getSupportFragmentManager(), "AddCategoryDialog");
    }

    /**
     * Hiển thị dialog sửa danh mục
     *
     * @param category Danh mục cần sửa
     */
    private void hienThiDialogSuaDanhMuc(Category category) {
        // Tạo và hiển thị dialog sửa danh mục
        EditCategoryDialog dialog = new EditCategoryDialog(
                this,
                category,
                database,
                () -> taiDanhMuc(category.getType()) // Callback để tải lại danh sách sau khi sửa
        );
        dialog.show(getSupportFragmentManager(), "EditCategoryDialog");
    }

    /**
     * Hiển thị dialog xác nhận xóa danh mục
     *
     * @param category Danh mục cần xóa
     */
    private void hienThiDialogXoaDanhMuc(Category category) {
        // Tạo và hiển thị dialog xác nhận xóa
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete_category)
                .setMessage(R.string.delete_category_confirmation)
                .setPositiveButton(R.string.delete, (dialog, which) -> xoaDanhMuc(category))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    /**
     * Xóa danh mục khỏi cơ sở dữ liệu
     *
     * @param category Danh mục cần xóa
     */
    private void xoaDanhMuc(Category category) {
        executorService.execute(() -> {
            try {
                // Thực hiện xóa danh mục từ cơ sở dữ liệu
                database.categoryDao().deleteCategory(category);
                // Cập nhật giao diện sau khi xóa thành công
                runOnUiThread(() -> {
                    // Hiển thị thông báo thành công
                    messageUtils.showSuccess(R.string.category_deleted);
                    // Tải lại danh sách danh mục
                    taiDanhMuc(category.getType());
                });
            } catch (Exception e) {
                // Xử lý lỗi nếu có
                Log.e(TAG, "Lỗi khi xóa danh mục: " + e.getMessage());
                runOnUiThread(() -> messageUtils.showError(R.string.error_deleting_category));
            }
        });
    }

    /**
     * Được gọi khi activity bị hủy
     * Đảm bảo giải phóng tài nguyên liên quan đến view binding
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Giải phóng binding để tránh rò rỉ bộ nhớ
        binding = null;
    }
}