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

import java.io.File;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * Adapter hiển thị danh sách giao dịch theo ngày
 */
public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder> {
    private static final String TAG = TransactionAdapter.class.getSimpleName();

    private final Context context;
    private List<DailyTransaction> dailyTransactions = new ArrayList<>();
    private List<Category> categories = new ArrayList<>();
    private OnTransactionDeleteListener deleteListener;

    // Interface để fragment truyền callback xử lý xóa giao dịch
    public interface OnTransactionDeleteListener {
        void onTransactionDelete(Transaction transaction);
    }

    public void setOnTransactionDeleteListener(OnTransactionDeleteListener listener) {
        this.deleteListener = listener;
    }

    public TransactionAdapter(Context context) {
        this.context = context;
    }

    /**
     * Cập nhật dữ liệu mới cho adapter
     */
    public void updateData(Collection<DailyTransaction> newDailyTransactions, List<Category> newCategories) {
        dailyTransactions = new ArrayList<>(newDailyTransactions);
        categories = new ArrayList<>(newCategories);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemTransactionHomeBinding binding = ItemTransactionHomeBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new TransactionViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
        try {
            // Tìm giao dịch tại vị trí hiện tại
            int currentPos = 0;
            DailyTransaction currentDaily = null;
            Transaction currentTransaction = null;

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

                // Xử lý sự kiện click
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
     * Hiển thị thông tin header của ngày
     */
    private void bindDailyHeader(TransactionViewHolder holder, DailyTransaction daily) {
        String dateText = Instant.ofEpochMilli(daily.getDate())
                .atZone(ZoneId.systemDefault())
                .format(DateTimeUtils.DATE_FORMATTER);
        holder.binding.tvDate.setText(dateText);
        holder.binding.tvTotalExpense.setText(String.format(Locale.getDefault(), "%,d",
                (long) daily.getTotalExpense()));
        holder.binding.tvTotalIncome.setText(String.format(Locale.getDefault(), "%,d",
                (long) daily.getTotalIncome()));
    }

    /**
     * Hiển thị chi tiết giao dịch
     */
    private void bindTransactionDetails(TransactionViewHolder holder, Transaction transaction,
                                        Category category) {
        if (category != null) {
            boolean isExpense = Constants.CATEGORY_TYPE_EXPENSE.equals(category.getType());
            int colorRes = isExpense ? R.color.expense_color : R.color.income_color;
            int color = ContextCompat.getColor(context, colorRes);

            // Set icon và màu sắc
            holder.binding.ivIcon.setText(category.getIcon());
            holder.binding.ivIcon.setTextColor(color);
            holder.binding.tvDescription.setText(category.getName());

            // Set số tiền
            long amount = Math.abs((long) transaction.getAmount());
            String amountText = String.format(Locale.getDefault(), "%,d", amount);
            holder.binding.tvAmount.setText((isExpense ? "-" : "+") + amountText);
            holder.binding.tvAmount.setTextColor(color);

            // Set ghi chú
            if (transaction.getDescription() != null && !transaction.getDescription().isEmpty()) {
                holder.binding.tvNote.setVisibility(View.VISIBLE);
                holder.binding.tvNote.setText(transaction.getDescription());
            } else {
                holder.binding.tvNote.setVisibility(View.GONE);
            }
        }
    }

    /**
     * Hiển thị dialog chi tiết giao dịch, thêm nút xóa
     */
    private void showTransactionDetail(Transaction transaction, Category category) {
        Dialog dialog = new Dialog(context);
        DialogTransactionDetailBinding dialogBinding =
                DialogTransactionDetailBinding.inflate(LayoutInflater.from(context));
        dialog.setContentView(dialogBinding.getRoot());

        // Thiết lập kích thước dialog
        setupDialogWindow(dialog);

        // Thiết lập dữ liệu
        boolean isExpense = category != null &&
                Constants.CATEGORY_TYPE_EXPENSE.equals(category.getType());

        dialogBinding.tvDialogTitle.setText(
                isExpense ? R.string.expense_detail : R.string.income_detail
        );
        dialogBinding.tvDialogCategory.setText(
                category != null ? category.getName() : "Unknown"
        );

        // Set số tiền
        long amount = Math.abs((long) transaction.getAmount());
        String amountText = String.format(Locale.getDefault(), "%,d", amount);
        dialogBinding.tvDialogAmount.setText((isExpense ? "-" : "+") + amountText);
        dialogBinding.tvDialogAmount.setTextColor(ContextCompat.getColor(context,
                isExpense ? R.color.expense_color : R.color.income_color));

        // Set ngày giờ
        String dateText = Instant.ofEpochMilli(transaction.getDate())
                .atZone(ZoneId.systemDefault())
                .format(DateTimeUtils.TRANSACTION_DATE_FORMATTER);
        dialogBinding.tvDialogDate.setText(dateText);

        // Set ghi chú
        String note = transaction.getDescription();
        dialogBinding.tvDialogNote.setText(
                note != null && !note.isEmpty() ? note : "Không có ghi chú"
        );

        // Hiển thị hình ảnh
        setupDialogImage(transaction.getImagePath(), dialogBinding.ivDialogImage);

        // Set sự kiện đóng dialog
        dialogBinding.btnClose.setOnClickListener(v -> dialog.dismiss());

        // Thêm nút xóa & xử lý xóa
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
     * Thiết lập cửa sổ dialog
     */
    private void setupDialogWindow(Dialog dialog) {
        Window window = dialog.getWindow();
        if (window != null) {
            DisplayMetrics metrics = new DisplayMetrics();
            window.getWindowManager().getDefaultDisplay().getMetrics(metrics);

            int width = (int) (metrics.widthPixels * 0.9);
            window.setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT);
            window.setBackgroundDrawable(new ColorDrawable(Color.WHITE));
        }
    }

    /**
     * Thiết lập hiển thị hình ảnh trong dialog
     */
    private void setupDialogImage(String imagePath, ImageView imageView) {
        if (imagePath != null && !imagePath.isEmpty()) {
            File imgFile = new File(imagePath);
            if (imgFile.exists()) {
                try {
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
        imageView.setVisibility(View.GONE);
    }

    /**
     * Hiển thị ảnh full màn hình
     */
    private void showFullScreenImage(Bitmap bitmap) {
        Dialog fullImageDialog = new Dialog(context,
                android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        ImageView fullImage = new ImageView(context);
        fullImage.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        fullImage.setScaleType(ImageView.ScaleType.FIT_CENTER);
        fullImageDialog.setContentView(fullImage);

        fullImage.setImageBitmap(bitmap);
        fullImage.setOnClickListener(view -> fullImageDialog.dismiss());
        fullImageDialog.show();
    }

    /**
     * Tìm Category theo ID
     */
    private Category findCategoryById(int categoryId) {
        return categories.stream()
                .filter(c -> c.getCategoryId() == categoryId)
                .findFirst()
                .orElse(null);
    }

    @Override
    public int getItemCount() {
        return dailyTransactions.stream()
                .mapToInt(daily -> daily.getTransactions().size())
                .sum();
    }

    /**
     * ViewHolder cho item giao dịch
     */
    static class TransactionViewHolder extends RecyclerView.ViewHolder {
        private final ItemTransactionHomeBinding binding;

        TransactionViewHolder(ItemTransactionHomeBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}