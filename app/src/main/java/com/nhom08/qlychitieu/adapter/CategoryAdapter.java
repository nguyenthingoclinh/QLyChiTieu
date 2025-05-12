package com.nhom08.qlychitieu.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.nhom08.qlychitieu.databinding.ItemCategoryBinding;
import com.nhom08.qlychitieu.mo_hinh.Category;

import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {
    private final Context context;
    private final List<Category> categories;
    private final OnCategoryClickListener listener;

    public interface OnCategoryClickListener {
        void onCategoryClick(Category category);
        void onCategoryMoreClick(Category category, View anchor);
    }

    public CategoryAdapter(Context context, List<Category> categories, OnCategoryClickListener listener) {
        this.context = context;
        this.categories = categories;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCategoryBinding binding = ItemCategoryBinding.inflate(
                LayoutInflater.from(context), parent, false
        );
        return new CategoryViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        holder.bind(categories.get(position));
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    public void updateCategories(List<Category> newCategories) {
        categories.clear();
        categories.addAll(newCategories);
        notifyDataSetChanged();
    }

    class CategoryViewHolder extends RecyclerView.ViewHolder {
        private final ItemCategoryBinding binding;

        CategoryViewHolder(ItemCategoryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Category category) {
            // Set icon
            binding.tvCategoryIcon.setText(category.getIcon());

            // Set name
            binding.tvCategoryName.setText(category.getName());

            // Set click listeners
            itemView.setOnClickListener(v -> listener.onCategoryClick(category));
            binding.tvCategoryMore.setOnClickListener(v ->
                    listener.onCategoryMoreClick(category, v));
        }
    }
}