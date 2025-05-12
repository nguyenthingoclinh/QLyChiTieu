package com.nhom08.qlychitieu.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.nhom08.qlychitieu.R;

import java.util.ArrayList;
import java.util.List;

public class IconAdapter extends RecyclerView.Adapter<IconAdapter.IconViewHolder> {
    private final Context context;
    private final OnIconSelectedListener listener;
    private List<String> icons = new ArrayList<>();
    private int selectedPosition = -1;

    public interface OnIconSelectedListener {
        void onIconSelected(String icon);
    }

    public IconAdapter(Context context, OnIconSelectedListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setIcons(List<String> icons) {
        this.icons = new ArrayList<>(icons);
        notifyDataSetChanged();
    }

    public void setSelectedIcon(String icon) {
        int position = icons.indexOf(icon);
        if (position != -1) {
            int oldSelected = selectedPosition;
            selectedPosition = position;
            if (oldSelected != -1) {
                notifyItemChanged(oldSelected);
            }
            notifyItemChanged(selectedPosition);
        }
    }
    public void clearSelection() {
        if (selectedPosition != -1) {
            int oldPosition = selectedPosition;
            selectedPosition = -1;
            notifyItemChanged(oldPosition);
        }
    }

    @NonNull
    @Override
    public IconViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_icon, parent, false);
        return new IconViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull IconViewHolder holder, int position) {
        String icon = icons.get(position);
        holder.bind(icon, position == selectedPosition);
    }

    @Override
    public int getItemCount() {
        return icons.size();
    }
    public String getSelectedIcon() {
        return selectedPosition >= 0 && selectedPosition < icons.size()
                ? icons.get(selectedPosition)
                : null;
    }

    class IconViewHolder extends RecyclerView.ViewHolder {
        TextView iconView;

        IconViewHolder(@NonNull View itemView) {
            super(itemView);
            iconView = itemView.findViewById(R.id.tvIcon);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    int oldSelected = selectedPosition;
                    selectedPosition = position;
                    if (oldSelected != -1) {
                        notifyItemChanged(oldSelected);
                    }
                    notifyItemChanged(selectedPosition);
                    listener.onIconSelected(icons.get(position));
                }
            });
        }

        void bind(String icon, boolean isSelected) {
            iconView.setText(icon);
            iconView.setSelected(isSelected);
        }
    }
}