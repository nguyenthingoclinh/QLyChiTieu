package com.nhom08.qlychitieu.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.nhom08.qlychitieu.R;
import com.nhom08.qlychitieu.databinding.ItemSpendingAlertBinding;
import com.nhom08.qlychitieu.mo_hinh.SpendingAlert;
import com.nhom08.qlychitieu.tien_ich.Constants;
import com.nhom08.qlychitieu.tien_ich.FormatUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter để hiển thị danh sách cảnh báo chi tiêu
 */
public class AlertAdapter extends RecyclerView.Adapter<AlertAdapter.AlertViewHolder> {

    private final Context context;
    private List<SpendingAlert> alerts = new ArrayList<>();
    private OnAlertActionListener listener;

    /**
     * Interface để xử lý các hành động trên cảnh báo
     */
    public interface OnAlertActionListener {
        void onAlertEdit(SpendingAlert alert);
        void onAlertDelete(SpendingAlert alert);
        void onAlertActiveChanged(SpendingAlert alert, boolean isActive);
    }

    public AlertAdapter(Context context) {
        this.context = context;
    }

    public void setOnAlertActionListener(OnAlertActionListener listener) {
        this.listener = listener;
    }

    public void setAlerts(List<SpendingAlert> alerts) {
        this.alerts = new ArrayList<>(alerts);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AlertViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemSpendingAlertBinding binding = ItemSpendingAlertBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new AlertViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull AlertViewHolder holder, int position) {
        SpendingAlert alert = alerts.get(position);

        // Thiết lập các thông tin cơ bản
        holder.binding.tvAlertTitle.setText(alert.getTitle());

        // Thiết lập loại cảnh báo
        String alertTypeText;
        int bgColor;
        switch (alert.getAlertType()) {
            case Constants.ALERT_TYPE_DAILY:
                alertTypeText = "HÀNG NGÀY";
                bgColor = R.color.colorPrimary;
                break;
            case Constants.ALERT_TYPE_WEEKLY:
                alertTypeText = "HÀNG TUẦN";
                bgColor = R.color.chart_color_7;
                break;
            case Constants.ALERT_TYPE_MONTHLY:
                alertTypeText = "HÀNG THÁNG";
                bgColor = android.R.color.holo_orange_dark;
                break;
            default:
                alertTypeText = "KHÔNG XÁC ĐỊNH";
                bgColor = android.R.color.darker_gray;
        }
        holder.binding.tvAlertType.setText(alertTypeText);
        holder.binding.tvAlertType.setBackgroundTintList(context.getColorStateList(bgColor));

        // Thiết lập ngưỡng cảnh báo
        String formattedThreshold = FormatUtils.formatCurrency(context, alert.getThreshold());
        holder.binding.tvThreshold.setText(String.format("Ngưỡng: %s", formattedThreshold));

        // Thiết lập trạng thái kích hoạt
        holder.binding.switchAlertActive.setChecked(alert.isActive());

        // Thiết lập sự kiện cho các nút tương tác
        holder.binding.switchAlertActive.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (listener != null) {
                listener.onAlertActiveChanged(alert, isChecked);
            }
        });

        holder.binding.btnEditAlert.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAlertEdit(alert);
            }
        });

        holder.binding.btnDeleteAlert.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAlertDelete(alert);
            }
        });
    }

    @Override
    public int getItemCount() {
        return alerts.size();
    }

    static class AlertViewHolder extends RecyclerView.ViewHolder {
        private final ItemSpendingAlertBinding binding;

        public AlertViewHolder(ItemSpendingAlertBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}