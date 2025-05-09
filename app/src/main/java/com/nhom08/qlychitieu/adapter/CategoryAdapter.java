package com.nhom08.qlychitieu.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.nhom08.qlychitieu.databinding.ItemCategoryBinding;
import com.nhom08.qlychitieu.mo_hinh.Category;

import java.util.ArrayList;
import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {
    private final Context context;
    private List<Category> categories;
    private final OnCategoryClickListener listener;

    public interface OnCategoryClickListener {
        void onCategoryClick(Category category);
        void onCategoryMoreClick(Category category, View anchor);
    }

    public CategoryAdapter(Context context, OnCategoryClickListener listener) {
        this.context = context;
        this.categories = new ArrayList<>();
        this.listener = listener;
    }

    public void updateCategories(List<Category> newCategories) {
        this.categories = new ArrayList<>(newCategories);
        // Chuyển đổi type sang tiếng Việt trước khi cập nhật
        for (Category category : this.categories) {
            if ("Expense".equals(category.getType())) {
                category.setDisplayType("Chi tiêu");
            } else if ("Income".equals(category.getType())) {
                category.setDisplayType("Thu nhập");
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCategoryBinding binding = ItemCategoryBinding.inflate(
                LayoutInflater.from(context), parent, false);
        return new CategoryViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        Category category = categories.get(position);
        holder.bind(category);
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    class CategoryViewHolder extends RecyclerView.ViewHolder {
        private final ItemCategoryBinding binding;

        CategoryViewHolder(ItemCategoryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Category category) {
            binding.tvCategoryIcon.setText(category.getIcon());
            binding.tvCategoryName.setText(category.getName());

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onCategoryClick(category);
                }
            });

            binding.tvCategoryMore.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onCategoryMoreClick(category, v);
                }
            });
        }
    }
}