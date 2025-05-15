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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ChartCategoryAdapter extends RecyclerView.Adapter<ChartCategoryAdapter.CategoryViewHolder> {

    private final Context context;
    private List<ChartCategoryInfo> categories = new ArrayList<>();
    private double totalAmount = 0;

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

        holder.binding.tvCategoryName.setText(item.getCategory().getName());
        holder.binding.tvCategoryIcon.setText(item.getCategory().getIcon());

        // Hiển thị số tiền
        holder.binding.tvAmount.setText(String.format(Locale.getDefault(), "%,d", (long) item.getAmount()));
        // Thêm điều kiện kiểm tra loại danh mục
        int colorRes;
        if (Constants.CATEGORY_TYPE_EXPENSE.equals(item.getCategory().getType())) {
            colorRes = R.color.expense_color; // Màu đỏ cho chi tiêu
        } else {
            colorRes = R.color.income_color; // Màu xanh cho thu nhập
        }

        holder.binding.tvAmount.setTextColor(ContextCompat.getColor(context, colorRes));
        holder.binding.tvPercentage.setTextColor(ContextCompat.getColor(context, colorRes));

        // Hiển thị phần trăm
        holder.binding.tvPercentage.setText(String.format(Locale.getDefault(), "%.1f%%", percentage));

        // Đặt màu cho biểu tượng
        holder.binding.tvCategoryIcon.setTextColor(item.getColor());
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    public void updateData(List<ChartCategoryInfo> newCategories, double total) {
        this.categories = newCategories;
        this.totalAmount = total;
        notifyDataSetChanged();
    }

    static class CategoryViewHolder extends RecyclerView.ViewHolder {
        private final ItemChartCategoryBinding binding;

        CategoryViewHolder(ItemChartCategoryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}