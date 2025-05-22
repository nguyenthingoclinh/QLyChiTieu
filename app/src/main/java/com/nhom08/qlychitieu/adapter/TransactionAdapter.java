package com.nhom08.qlychitieu.adapter;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.nhom08.qlychitieu.R;
import com.nhom08.qlychitieu.databinding.DialogTransactionDetailBinding;
import com.nhom08.qlychitieu.databinding.ItemTransactionHomeBinding;
import com.nhom08.qlychitieu.mo_hinh.Category;
import com.nhom08.qlychitieu.mo_hinh.DailyTransaction;
import com.nhom08.qlychitieu.mo_hinh.Transaction;
import com.nhom08.qlychitieu.tien_ich.Constants;
import com.nhom08.qlychitieu.tien_ich.DateTimeUtils;
import com.nhom08.qlychitieu.tien_ich.FormatUtils;

import java.io.File;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * Adapter hiển thị danh sách giao dịch theo ngày
 * - Hiển thị danh sách giao dịch được nhóm theo ngày
 * - Mỗi ngày hiển thị tổng chi tiêu và thu nhập
 * - Hỗ trợ xem chi tiết giao dịch khi nhấn vào item
 * - Cho phép xóa giao dịch từ dialog chi tiết
 * - Hiển thị hình ảnh biên lai nếu có và cho phép xem hình ảnh full màn hình
 */
public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder> {
    private static final String TAG = TransactionAdapter.class.getSimpleName();

    // Context để truy cập tài nguyên
    private final Context context;

    // Danh sách giao dịch theo ngày
    private List<DailyTransaction> dailyTransactions = new ArrayList<>();

    // Danh sách danh mục để hiển thị tên và icon
    private List<Category> categories = new ArrayList<>();

    // Listener xử lý sự kiện xóa giao dịch
    private OnTransactionDeleteListener deleteListener;

    /**
     * Interface để fragment truyền callback xử lý xóa giao dịch
     * Giúp tách biệt logic xóa khỏi adapter và giao cho fragment/activity quản lý
     */
    public interface OnTransactionDeleteListener {
        /**
         * Được gọi khi người dùng xác nhận xóa một giao dịch
         * @param transaction Giao dịch cần xóa
         */
        void onTransactionDelete(Transaction transaction);
    }

    /**
     * Thiết lập listener để xử lý sự kiện xóa giao dịch
     * @param listener Đối tượng listener xử lý sự kiện xóa
     */
    public void setOnTransactionDeleteListener(OnTransactionDeleteListener listener) {
        this.deleteListener = listener;
    }

    /**
     * Constructor khởi tạo adapter với context
     * @param context Context của activity/fragment sử dụng adapter
     */
    public TransactionAdapter(Context context) {
        this.context = context;
    }

    /**
     * Cập nhật dữ liệu mới cho adapter
     * @param newDailyTransactions Danh sách giao dịch theo ngày mới
     * @param newCategories Danh sách danh mục mới
     */
    public void updateData(Collection<DailyTransaction> newDailyTransactions, List<Category> newCategories) {
        dailyTransactions = new ArrayList<>(newDailyTransactions);
        categories = new ArrayList<>(newCategories);
        notifyDataSetChanged();
    }

    /**
     * Tạo ViewHolder mới cho mỗi item giao dịch
     */
    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Sử dụng View Binding để tạo view từ layout XML
        ItemTransactionHomeBinding binding = ItemTransactionHomeBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new TransactionViewHolder(binding);
    }

    /**
     * Gán dữ liệu vào ViewHolder tại vị trí xác định
     * Hiển thị header cho mỗi ngày và chi tiết cho từng giao dịch
     */
    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
        try {
            // Tìm giao dịch tại vị trí hiện tại
            int currentPos = 0;
            DailyTransaction currentDaily = null;
            Transaction currentTransaction = null;

            // Duyệt danh sách để tìm giao dịch ở vị trí position
            for (DailyTransaction daily : dailyTransactions) {
                if (position < currentPos + daily.getTransactions().size()) {
                    currentDaily = daily;
                    currentTransaction = daily.getTransactions().get(position - currentPos);
                    break;
                }
                currentPos += daily.getTransactions().size();
            }

            if (currentDaily != null && currentTransaction != null) {
                Category category = findCategoryById(currentTransaction.getCategoryId());

                // Hiển thị header cho giao dịch đầu tiên của mỗi ngày
                holder.binding.layoutHeader.setVisibility(
                        position == currentPos ? View.VISIBLE : View.GONE
                );

                if (position == currentPos) {
                    bindDailyHeader(holder, currentDaily);
                }

                // Hiển thị chi tiết giao dịch
                bindTransactionDetails(holder, currentTransaction, category);

                // Xử lý sự kiện click vào item để hiển thị chi tiết
                Transaction finalCurrentTransaction = currentTransaction;
                Category finalCategory = category;
                holder.itemView.setOnClickListener(v ->
                        showTransactionDetail(finalCurrentTransaction, finalCategory));
            }
        } catch (Exception e) {
            Log.e(TAG, "Lỗi trong onBindViewHolder: " + e.getMessage());
        }
    }

    /**
     * Hiển thị thông tin header của ngày bao gồm ngày tháng và tổng chi tiêu/thu nhập
     * @param holder ViewHolder chứa các thành phần giao diện
     * @param daily Đối tượng DailyTransaction chứa thông tin tổng hợp của ngày
     */
    private void bindDailyHeader(TransactionViewHolder holder, DailyTransaction daily) {
        // Hiển thị ngày tháng
        String dateText = Instant.ofEpochMilli(daily.getDate())
                .atZone(ZoneId.systemDefault())
                .format(DateTimeUtils.DATE_FORMATTER);
        holder.binding.tvDate.setText(dateText);

        // Hiển thị tổng chi tiêu và thu nhập sử dụng FormatUtils
        // Sử dụng phương thức formatCurrency với tham số context để áp dụng cấu hình định dạng tiền tệ
        holder.binding.tvTotalExpense.setText(FormatUtils.formatCurrency(context, daily.getTotalExpense()));
        holder.binding.tvTotalIncome.setText(FormatUtils.formatCurrency(context, daily.getTotalIncome()));
    }

    /**
     * Hiển thị chi tiết của một giao dịch
     * @param holder ViewHolder chứa các thành phần giao diện
     * @param transaction Giao dịch cần hiển thị
     * @param category Danh mục của giao dịch
     */
    private void bindTransactionDetails(TransactionViewHolder holder, Transaction transaction,
                                        Category category) {
        if (category != null) {
            boolean isExpense = Constants.CATEGORY_TYPE_EXPENSE.equals(category.getType());
            int colorRes = isExpense ? R.color.expense_color : R.color.income_color;
            int color = ContextCompat.getColor(context, colorRes);

            // Hiển thị icon và màu sắc tương ứng với loại giao dịch
            holder.binding.ivIcon.setText(category.getIcon());
            holder.binding.ivIcon.setTextColor(color);
            holder.binding.tvDescription.setText(category.getName());

            // Hiển thị số tiền với định dạng và màu sắc phù hợp
            // Sử dụng formatCurrency với context để áp dụng cấu hình định dạng của người dùng
            double amount = Math.abs(transaction.getAmount());
            String prefix = isExpense ? "-" : "+";
            String amountText = prefix + FormatUtils.formatCurrency(context, amount);
            holder.binding.tvAmount.setText(amountText);
            holder.binding.tvAmount.setTextColor(color);

            // Hiển thị ghi chú nếu có
            if (transaction.getDescription() != null && !transaction.getDescription().isEmpty()) {
                holder.binding.tvNote.setVisibility(View.VISIBLE);
                holder.binding.tvNote.setText(transaction.getDescription());
            } else {
                holder.binding.tvNote.setVisibility(View.GONE);
            }
        }
    }

    /**
     * Hiển thị dialog chi tiết giao dịch khi người dùng nhấn vào một giao dịch
     * Dialog này hiển thị đầy đủ thông tin và cho phép người dùng xóa giao dịch
     * @param transaction Giao dịch cần hiển thị chi tiết
     * @param category Danh mục của giao dịch
     */
    private void showTransactionDetail(Transaction transaction, Category category) {
        Dialog dialog = new Dialog(context);
        DialogTransactionDetailBinding dialogBinding =
                DialogTransactionDetailBinding.inflate(LayoutInflater.from(context));
        dialog.setContentView(dialogBinding.getRoot());

        // Thiết lập kích thước và giao diện cho dialog
        setupDialogWindow(dialog);

        // Xác định loại giao dịch (chi tiêu hoặc thu nhập)
        boolean isExpense = category != null &&
                Constants.CATEGORY_TYPE_EXPENSE.equals(category.getType());

        // Hiển thị tiêu đề dialog dựa vào loại giao dịch
        dialogBinding.tvDialogTitle.setText(
                isExpense ? R.string.expense_detail : R.string.income_detail
        );

        // Hiển thị tên danh mục
        dialogBinding.tvDialogCategory.setText(
                category != null ? category.getName() : "Unknown"
        );

        // Hiển thị số tiền với định dạng và màu sắc tương ứng
        // Sử dụng formatCurrency với context để áp dụng cấu hình định dạng của người dùng
        double amount = Math.abs(transaction.getAmount());
        String prefix = isExpense ? "-" : "+";
        String amountText = prefix + FormatUtils.formatCurrency(context, amount);
        dialogBinding.tvDialogAmount.setText(amountText);
        dialogBinding.tvDialogAmount.setTextColor(ContextCompat.getColor(context,
                isExpense ? R.color.expense_color : R.color.income_color));

        // Hiển thị ngày giờ sử dụng FormatUtils
        // FormatDateTime sử dụng định dạng cố định từ DateTimeUtils
        String dateText = FormatUtils.formatDateTime(transaction.getDate());
        dialogBinding.tvDialogDate.setText(dateText);

        // Hiển thị ghi chú
        String note = transaction.getDescription();
        dialogBinding.tvDialogNote.setText(
                note != null && !note.isEmpty() ? note : "Không có ghi chú"
        );

        // Hiển thị hình ảnh biên lai nếu có
        setupDialogImage(transaction.getImagePath(), dialogBinding.ivDialogImage);

        // Nút đóng dialog
        dialogBinding.btnClose.setOnClickListener(v -> dialog.dismiss());

        // Thêm nút xóa và xử lý sự kiện xóa giao dịch
        dialogBinding.btnDelete.setVisibility(View.VISIBLE);
        dialogBinding.btnDelete.setOnClickListener(v -> {
            new android.app.AlertDialog.Builder(context)
                    .setTitle("Xác nhận xóa")
                    .setMessage("Bạn có chắc chắn muốn xóa giao dịch này không?")
                    .setPositiveButton("Xóa", (dialogInterface, i) -> {
                        if (deleteListener != null) {
                            deleteListener.onTransactionDelete(transaction);
                        }
                        dialog.dismiss();
                    })
                    .setNegativeButton("Hủy", null)
                    .show();
        });

        dialog.show();
    }

    /**
     * Thiết lập cửa sổ dialog với kích thước và background phù hợp
     * @param dialog Dialog cần thiết lập
     */
    private void setupDialogWindow(Dialog dialog) {
        Window window = dialog.getWindow();
        if (window != null) {
            // Lấy kích thước màn hình để tính toán kích thước dialog
            DisplayMetrics metrics = new DisplayMetrics();
            window.getWindowManager().getDefaultDisplay().getMetrics(metrics);

            // Đặt chiều rộng dialog bằng 90% chiều rộng màn hình
            int width = (int) (metrics.widthPixels * 0.9);
            window.setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT);
            window.setBackgroundDrawable(new ColorDrawable(Color.WHITE));
        }
    }

    /**
     * Thiết lập hiển thị hình ảnh trong dialog và xử lý sự kiện xem ảnh full màn hình
     * @param imagePath Đường dẫn đến file hình ảnh
     * @param imageView ImageView để hiển thị hình ảnh
     */
    private void setupDialogImage(String imagePath, ImageView imageView) {
        if (imagePath != null && !imagePath.isEmpty()) {
            File imgFile = new File(imagePath);
            if (imgFile.exists()) {
                try {
                    // Decode bitmap từ file
                    Bitmap bitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                    if (bitmap != null) {
                        imageView.setImageBitmap(bitmap);
                        imageView.setVisibility(View.VISIBLE);

                        // Thêm sự kiện click để xem ảnh full màn hình
                        imageView.setOnClickListener(v -> showFullScreenImage(bitmap));
                        return;
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Lỗi load ảnh: " + e.getMessage());
                }
            }
        }
        // Ẩn ImageView nếu không có ảnh hoặc gặp lỗi khi load ảnh
        imageView.setVisibility(View.GONE);
    }

    /**
     * Hiển thị ảnh full màn hình khi người dùng click vào ảnh trong dialog
     * @param bitmap Bitmap của hình ảnh cần hiển thị
     */
    private void showFullScreenImage(Bitmap bitmap) {
        // Tạo dialog full màn hình không có tiêu đề
        Dialog fullImageDialog = new Dialog(context,
                android.R.style.Theme_Black_NoTitleBar_Fullscreen);

        // Tạo ImageView để hiển thị ảnh
        ImageView fullImage = new ImageView(context);
        fullImage.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        fullImage.setScaleType(ImageView.ScaleType.FIT_CENTER);
        fullImageDialog.setContentView(fullImage);

        // Hiển thị ảnh và thêm sự kiện click để đóng dialog
        fullImage.setImageBitmap(bitmap);
        fullImage.setOnClickListener(view -> fullImageDialog.dismiss());
        fullImageDialog.show();
    }

    /**
     * Tìm danh mục theo ID
     * @param categoryId ID của danh mục cần tìm
     * @return Đối tượng Category hoặc null nếu không tìm thấy
     */
    private Category findCategoryById(int categoryId) {
        return categories.stream()
                .filter(c -> c.getCategoryId() == categoryId)
                .findFirst()
                .orElse(null);
    }

    /**
     * Trả về tổng số item trong adapter
     * Được tính bằng tổng số giao dịch trong tất cả các ngày
     */
    @Override
    public int getItemCount() {
        return dailyTransactions.stream()
                .mapToInt(daily -> daily.getTransactions().size())
                .sum();
    }

    /**
     * ViewHolder lưu trữ và quản lý các thành phần giao diện cho một item giao dịch
     */
    static class TransactionViewHolder extends RecyclerView.ViewHolder {
        private final ItemTransactionHomeBinding binding;

        /**
         * Constructor khởi tạo ViewHolder với binding
         * @param binding View binding của layout item
         */
        TransactionViewHolder(ItemTransactionHomeBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}