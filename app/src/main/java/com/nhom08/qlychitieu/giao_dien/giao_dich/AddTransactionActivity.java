package com.nhom08.qlychitieu.giao_dien.giao_dich;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.tabs.TabLayout;
import com.nhom08.qlychitieu.MyApplication;
import com.nhom08.qlychitieu.R;
import com.nhom08.qlychitieu.csdl.AppDatabase;
import com.nhom08.qlychitieu.databinding.ActivityAddTransactionBinding;
import com.nhom08.qlychitieu.databinding.ItemCategoryTransactionBinding;
import com.nhom08.qlychitieu.mo_hinh.Category;
import com.nhom08.qlychitieu.mo_hinh.Transaction;
import com.nhom08.qlychitieu.tien_ich.CalculatorHandler;
import com.nhom08.qlychitieu.tien_ich.Constants;
import com.nhom08.qlychitieu.tien_ich.FormatUtils;
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

/**
 * Màn hình thêm giao dịch mới, cho phép người dùng nhập thông tin chi tiêu hoặc thu nhập
 * bao gồm: số tiền, danh mục, ngày tháng, mô tả và hình ảnh biên lai.
 */
public class AddTransactionActivity extends AppCompatActivity {
    private static final String TAG = AddTransactionActivity.class.getSimpleName();
    private ActivityAddTransactionBinding binding;
    private ImageUtils imageUtils;
    private MessageUtils messageUtils;
    private CalculatorHandler calculatorHandler;
    private CategoryAdapter categoryAdapter;
    private AppDatabase database;
    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    private Category selectedCategory;
    private long selectedDate = System.currentTimeMillis();
    private String imagePath = "";
    private int userId;
    private ActivityResultLauncher<Intent> pickImageLauncher;

    // Biến để kiểm soát định dạng tiền tệ
    private boolean isFormattingAmount = false;

    /**
     * Được gọi khi activity được tạo lần đầu
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityAddTransactionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        khoiTaoCacThanhPhan();
        thietLapGiaoDien();
        capNhatHienThiNgay(new Date(selectedDate));
        taiDanhMuc(Constants.CATEGORY_TYPE_EXPENSE); // Load default categories
    }

    /**
     * Khởi tạo các thành phần cần thiết cho activity
     */
    private void khoiTaoCacThanhPhan() {
        MyApplication app = MyApplication.getInstance();
        database = app.getDatabase();
        userId = app.getCurrentUserId();
        executorService = app.getExecutorService();

        imageUtils = new ImageUtils(this);
        messageUtils = new MessageUtils(this);
        calculatorHandler = new CalculatorHandler();

        thietLapChonHinhAnh();
        thietLapMayTinh();
        thietLapChonNgay();
        thietLapAdapterDanhMuc();
        thietLapDinhDangTienTe();
    }

    /**
     * Thiết lập định dạng tiền tệ cho số tiền hiển thị
     */
    private void thietLapDinhDangTienTe() {
        // Thêm TextWatcher cho TextView hiển thị số tiền
        binding.tvAmount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Không cần xử lý
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Không cần xử lý
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Tránh vòng lặp vô hạn khi định dạng
                if (isFormattingAmount) return;

                String rawAmount = s.toString().replace(",", "")
                        .replace(".", "")
                        .replace(" ", "")
                        .replace("VND", "")
                        .replace("₫", "");

                // Nếu là số 0 hoặc rỗng, hiển thị là 0
                if (rawAmount.isEmpty() || "0".equals(rawAmount)) {
                    return;
                }

                try {
                    // Chuyển đổi số tiền thành double
                    double amount = Double.parseDouble(rawAmount);

                    // Đánh dấu đang trong quá trình định dạng
                    isFormattingAmount = true;

                    // Định dạng số tiền và hiển thị
                    String formattedAmount = FormatUtils.formatCurrency(AddTransactionActivity.this, amount);

                    // Hiển thị số tiền đã định dạng
                    binding.tvAmount.setText(formattedAmount);

                    // Đã hoàn tất định dạng
                    isFormattingAmount = false;
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Error formatting amount: " + e.getMessage());
                }
            }
        });
    }

    /**
     * Thiết lập các thành phần giao diện cơ bản
     */
    private void thietLapGiaoDien() {
        // Setup click listeners
        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnSave.setOnClickListener(v -> luuGiaoDich());
    }

    /**
     * Thiết lập bộ chọn hình ảnh
     */
    private void thietLapChonHinhAnh() {
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        xuLyKetQuaChonHinhAnh(result.getData().getData());
                    }
                }
        );

        binding.layoutAddPhoto.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            pickImageLauncher.launch(intent);
        });
    }

    /**
     * Thiết lập bộ chọn ngày
     */
    private void thietLapChonNgay() {
        binding.layoutDate.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(selectedDate > 0 ? selectedDate : System.currentTimeMillis());

            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    this,
                    (view, year, month, dayOfMonth) -> {
                        calendar.set(year, month, dayOfMonth);
                        selectedDate = calendar.getTimeInMillis();
                        capNhatHienThiNgay(calendar.getTime());
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.show();
        });
    }

    /**
     * Thiết lập bộ tính toán
     */
    private void thietLapMayTinh() {
        thietLapCacNutSo();
        thietLapCacNutPhepToan();
        thietLapCacNutHanhDong();
    }

    /**
     * Thiết lập các nút số cho máy tính
     */
    private void thietLapCacNutSo() {
        int[] numberButtonIds = {
                R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3,
                R.id.btn4, R.id.btn5, R.id.btn6, R.id.btn7,
                R.id.btn8, R.id.btn9, R.id.btn000, R.id.btnDot
        };

        for (int id : numberButtonIds) {
            binding.getRoot().findViewById(id).setOnClickListener(v -> {
                String value = ((android.widget.Button) v).getText().toString();
                xuLyNhapSo(value);
            });
        }
    }

    /**
     * Xử lý khi người dùng nhấn các nút số
     * @param value Giá trị của nút được nhấn
     */
    private void xuLyNhapSo(String value) {
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
        capNhatHienThiSoTien();
    }

    /**
     * Thiết lập các nút phép toán (+, -)
     */
    private void thietLapCacNutPhepToan() {
        binding.btnPlus.setOnClickListener(v ->
                xuLyPhepToan("+"));
        binding.btnMinus.setOnClickListener(v ->
                xuLyPhepToan("-"));
    }

    /**
     * Xử lý khi người dùng nhấn các nút phép toán
     * @param operator Phép toán được chọn (+, -)
     */
    private void xuLyPhepToan(String operator) {
        calculatorHandler.appendOperator(operator);
        capNhatHienThiSoTien();
    }

    /**
     * Thiết lập các nút hành động (Clear, Done)
     */
    private void thietLapCacNutHanhDong() {
        binding.btnClear.setOnClickListener(v -> {
            calculatorHandler.clear();
            capNhatHienThiSoTien();
        });
        binding.btnDone.setOnClickListener(v -> luuGiaoDich());
    }

    /**
     * Thiết lập adapter cho danh sách danh mục
     */
    private void thietLapAdapterDanhMuc() {
        // Khởi tạo adapter với danh sách rỗng
        categoryAdapter = new CategoryAdapter(new ArrayList<>());
        binding.gridCategories.setAdapter(categoryAdapter);

        // Setup tab listener để chuyển đổi giữa Chi tiêu và Thu nhập
        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                // Reset tất cả các input khi chuyển tab
                lamMoiDauVao();
                // Load danh mục tương ứng với tab được chọn
                taiDanhMuc(tab.getPosition() == 0 ? "Expense" : "Income");
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

    /**
     * Cập nhật hiển thị số tiền trên giao diện
     * Định dạng số tiền theo định dạng tiền tệ
     */
    private void capNhatHienThiSoTien() {
        String rawAmount = calculatorHandler.getCurrentInput();

        if (rawAmount.isEmpty()) {
            binding.tvAmount.setText("0");
            return;
        }

        try {
            // Thử chuyển đổi thành số để định dạng
            double amount = calculatorHandler.calculate();

            // Sử dụng FormatUtils để định dạng tiền tệ
            String formattedAmount = FormatUtils.formatCurrency(this, amount);

            // Hiển thị số tiền đã định dạng
            binding.tvAmount.setText(formattedAmount);
        } catch (NumberFormatException e) {
            // Nếu chuỗi hiện tại không thể chuyển thành số (ví dụ: "5+"),
            // hiển thị nguyên bản
            binding.tvAmount.setText(rawAmount);
        }
    }

    /**
     * Xử lý kết quả sau khi chọn hình ảnh
     * @param imageUri URI của hình ảnh được chọn
     */
    private void xuLyKetQuaChonHinhAnh(Uri imageUri) {
        if (imageUri == null) return;

        String savedPath = imageUtils.saveImage(imageUri);
        if (savedPath != null) {
            imagePath = savedPath;
            binding.ivReceipt.setImageURI(Uri.fromFile(new File(savedPath)));
            binding.ivReceipt.setVisibility(View.VISIBLE);
            binding.tvAttachPhoto.setText(R.string.photo_attached);
            // Thay đổi icon thay vì ẩn nó
            binding.tvIconAddPhoto.setText(R.string.icon_camera_added);
        } else {
            messageUtils.showError(R.string.error_saving_image);
        }
    }

    /**
     * Cập nhật hiển thị ngày trên giao diện
     * @param date Đối tượng Date cần hiển thị
     */
    private void capNhatHienThiNgay(Date date) {
        String formattedDate = new SimpleDateFormat(
                Constants.DEFAULT_DATE_FORMAT,
                Locale.getDefault()
        ).format(date);
        binding.tvDate.setText(formattedDate);
    }

    /**
     * Tải danh sách danh mục từ cơ sở dữ liệu
     * @param type Loại danh mục (Chi tiêu hoặc Thu nhập)
     */
    private void taiDanhMuc(String type) {
        executorService.execute(() -> {
            try {
                List<Category> categories = database.categoryDao()
                        .getCategoriesByType(userId, type);
                runOnUiThread(() -> capNhatDanhSachDanhMuc(categories));
            } catch (Exception e) {
                Log.e(TAG, "Error loading categories: " + e.getMessage());
                runOnUiThread(() ->
                        messageUtils.showError(R.string.error_loading_categories));
            }
        });
    }

    /**
     * Cập nhật danh sách danh mục trên giao diện
     * @param categories Danh sách danh mục cần hiển thị
     */
    private void capNhatDanhSachDanhMuc(List<Category> categories) {
        if (categoryAdapter == null) return;

        categoryAdapter.updateCategories(categories != null ? categories : new ArrayList<>());

        assert categories != null;
        if (categories.isEmpty()) {
            binding.tvNoCategories.setVisibility(View.VISIBLE);
            binding.gridCategories.setVisibility(View.GONE);
        } else {
            binding.tvNoCategories.setVisibility(View.GONE);
            binding.gridCategories.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Lưu thông tin giao dịch mới
     */
    private void luuGiaoDich(){
        if (!kiemTraDauVao()) return;

        try {
            double amount = calculatorHandler.calculate();
            if (amount == 0) {
                messageUtils.showError(R.string.error_invalid_amount);
                return;
            }

            if (Constants.CATEGORY_TYPE_EXPENSE.equals(selectedCategory.getType())) {
                amount = -Math.abs(amount);
            }

            final Transaction transaction = new Transaction(
                    userId,
                    selectedCategory.getCategoryId(),
                    amount,
                    selectedDate != 0 ? selectedDate : System.currentTimeMillis(),
                    binding.etDescription.getText().toString().trim(),
                    imagePath
            );

            luuGiaoDichVaoDatabase(transaction);

        } catch (NumberFormatException e) {
            messageUtils.showError(R.string.error_invalid_amount);
        }
    }

    /**
     * Kiểm tra tính hợp lệ của dữ liệu đầu vào
     * @return true nếu dữ liệu hợp lệ, false nếu không
     */
    private boolean kiemTraDauVao() {
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

    /**
     * Lưu giao dịch vào cơ sở dữ liệu
     * @param transaction Đối tượng giao dịch cần lưu
     */
    private void luuGiaoDichVaoDatabase(Transaction transaction) {
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

    /**
     * Làm mới tất cả các trường nhập liệu
     */
    private void lamMoiDauVao() {
        binding.etDescription.setText("");
        calculatorHandler.clear();
        capNhatHienThiSoTien();
        selectedCategory = null;
        categoryAdapter.setSelectedCategory(null);
        selectedDate = System.currentTimeMillis(); // Đặt ngày về ngày hiện tại thay vì 0
        capNhatHienThiNgay(new Date(selectedDate)); // Cập nhật hiển thị ngày
        imagePath = "";
        binding.ivReceipt.setVisibility(View.GONE);
        binding.tvAttachPhoto.setText(R.string.attach_photo);
        // Reset về icon ban đầu
        binding.tvIconAddPhoto.setText(R.string.icon_add_a_photo);
    }

    /**
     * Được gọi khi activity bị hủy
     * Đảm bảo giải phóng tài nguyên liên quan đến view binding
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }

    /**
     * Adapter hiển thị danh sách danh mục trong lưới
     */
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