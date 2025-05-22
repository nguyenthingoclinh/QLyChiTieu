package com.nhom08.qlychitieu.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.nhom08.qlychitieu.R;
import com.nhom08.qlychitieu.databinding.ItemChartCategoryBinding;
import com.nhom08.qlychitieu.mo_hinh.ChartCategoryInfo;
import com.nhom08.qlychitieu.tien_ich.Constants;
import com.nhom08.qlychitieu.tien_ich.FormatUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter hiển thị danh sách danh mục với tỷ lệ phần trăm và màu sắc cho biểu đồ
 * Được sử dụng trong màn hình thống kê để hiển thị chi tiết từng danh mục
 */
public class ChartCategoryAdapter extends RecyclerView.Adapter<ChartCategoryAdapter.CategoryViewHolder> {

    // Context để truy cập tài nguyên
    private final Context context;

    // Danh sách thông tin danh mục cho biểu đồ
    private List<ChartCategoryInfo> categories = new ArrayList<>();

    // Tổng số tiền của tất cả danh mục
    private double totalAmount = 0;

    /**
     * Constructor khởi tạo adapter
     * @param context Context của activity/fragment sử dụng adapter
     */
    public ChartCategoryAdapter(Context context) {
        this.context = context;
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemChartCategoryBinding binding = ItemChartCategoryBinding.inflate(
                LayoutInflater.from(context), parent, false);
        return new CategoryViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        ChartCategoryInfo item = categories.get(position);

        // Tính phần trăm
        double percentage = totalAmount > 0 ? (item.getAmount() / totalAmount) * 100 : 0;

        // Hiển thị tên và icon danh mục
        holder.binding.tvCategoryName.setText(item.getCategory().getName());
        holder.binding.tvCategoryIcon.setText(item.getCategory().getIcon());

        // Hiển thị số tiền với định dạng phù hợp từ FormatUtils
        holder.binding.tvAmount.setText(FormatUtils.formatCurrency(context, item.getAmount()));

        // Thêm điều kiện kiểm tra loại danh mục để hiển thị màu sắc phù hợp
        int colorRes;
        if (Constants.CATEGORY_TYPE_EXPENSE.equals(item.getCategory().getType())) {
            colorRes = R.color.expense_color; // Màu đỏ cho chi tiêu
        } else {
            colorRes = R.color.income_color; // Màu xanh cho thu nhập
        }

        // Áp dụng màu sắc cho số tiền và phần trăm
        holder.binding.tvAmount.setTextColor(ContextCompat.getColor(context, colorRes));
        holder.binding.tvPercentage.setTextColor(ContextCompat.getColor(context, colorRes));

        // Hiển thị phần trăm sử dụng FormatUtils
        holder.binding.tvPercentage.setText(FormatUtils.formatPercent(percentage/100, 1));

        // Đặt màu cho biểu tượng danh mục từ thông tin màu đã được định nghĩa
        holder.binding.tvCategoryIcon.setTextColor(item.getColor());
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    /**
     * Cập nhật dữ liệu mới cho adapter
     * @param newCategories Danh sách danh mục mới
     * @param total Tổng số tiền của tất cả danh mục
     */
    public void updateData(List<ChartCategoryInfo> newCategories, double total) {
        this.categories = newCategories;
        this.totalAmount = total;
        notifyDataSetChanged();
    }

    /**
     * ViewHolder lưu trữ các thành phần giao diện cho một item danh mục
     */
    static class CategoryViewHolder extends RecyclerView.ViewHolder {
        private final ItemChartCategoryBinding binding;

        CategoryViewHolder(ItemChartCategoryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}