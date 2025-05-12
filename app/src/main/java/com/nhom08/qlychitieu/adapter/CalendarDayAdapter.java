package com.nhom08.qlychitieu.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.nhom08.qlychitieu.R;
import com.nhom08.qlychitieu.databinding.ItemCalendarDayBinding;
import com.nhom08.qlychitieu.mo_hinh.Category;
import com.nhom08.qlychitieu.mo_hinh.Transaction;
import com.nhom08.qlychitieu.tien_ich.Constants;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Adapter cho RecyclerView hiển thị lưới ngày trong tháng (lịch).
 */
public class CalendarDayAdapter extends RecyclerView.Adapter<CalendarDayAdapter.DayViewHolder> {

    public interface OnDayClickListener {
        void onDayClick(int day, int year, int month);
    }

    private final Context context;
    private final OnDayClickListener listener;

    private int year;
    private int month;
    private int daysInMonth;
    private int firstDayOfWeek;
    private double dailyBudget;

    private Map<Integer, DaySummary> daySummaryMap = new HashMap<>();

    public static class DaySummary {
        public double expense;
        public double income;
        public boolean isOverBudget;
    }

    public CalendarDayAdapter(Context context, OnDayClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    @NonNull
    @Override
    public DayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCalendarDayBinding binding = ItemCalendarDayBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new DayViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull DayViewHolder holder, int position) {
        int dayOffset = firstDayOfWeek - 1;
        int day = position - dayOffset + 1;

        if (position < dayOffset || day > daysInMonth) {
            holder.binding.getRoot().setVisibility(View.INVISIBLE);
            return;
        }

        holder.binding.getRoot().setVisibility(View.VISIBLE);
        holder.binding.tvDay.setText(String.valueOf(day));

        // Hiển thị thu chi nếu có
        DaySummary summary = daySummaryMap.get(day);
        if (summary != null) {
            NumberFormat nf = NumberFormat.getInstance(new Locale("vi", "VN"));

            if (summary.expense > 0) {
                holder.binding.tvExpense.setVisibility(View.VISIBLE);
                holder.binding.tvExpense.setText(nf.format((long)summary.expense));
            } else {
                holder.binding.tvExpense.setVisibility(View.GONE);
            }

            if (summary.income > 0) {
                holder.binding.tvIncome.setVisibility(View.VISIBLE);
                holder.binding.tvIncome.setText(nf.format((long)summary.income));
            } else {
                holder.binding.tvIncome.setVisibility(View.GONE);
            }

            // Set background color dựa vào ngân sách
            int bgColor = summary.isOverBudget ?
                    R.color.calendar_exceed_bg :
                    R.color.calendar_within_bg;
            holder.binding.cardView.setCardBackgroundColor(
                    ContextCompat.getColor(context, bgColor));
        } else {
            holder.binding.tvExpense.setVisibility(View.GONE);
            holder.binding.tvIncome.setVisibility(View.GONE);
            holder.binding.cardView.setCardBackgroundColor(Color.WHITE);
        }

        holder.binding.getRoot().setOnClickListener(v -> {
            if (listener != null) listener.onDayClick(day, year, month);
        });
    }

    @Override
    public int getItemCount() {
        return daysInMonth + (firstDayOfWeek - 1);
    }

    public void setMonth(int year, int month, List<Transaction> transactions,
                         List<Category> categories, double monthlyBudget) {
        this.year = year;
        this.month = month;

        // Tính số ngày và thứ bắt đầu của tháng
        Calendar cal = Calendar.getInstance();
        cal.set(year, month, 1);
        daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK);

        // Tính ngân sách trung bình mỗi ngày
        dailyBudget = monthlyBudget / daysInMonth;

        // Gom nhóm giao dịch theo ngày
        daySummaryMap.clear();
        for (Transaction t : transactions) {
            cal.setTimeInMillis(t.getDate());
            int day = cal.get(Calendar.DAY_OF_MONTH);

            DaySummary summary = daySummaryMap.computeIfAbsent(day, k -> new DaySummary());

            Category cat = categories.stream()
                    .filter(c -> c.getCategoryId() == t.getCategoryId())
                    .findFirst()
                    .orElse(null);

            if (cat != null) {
                if (cat.getType().equals(Constants.CATEGORY_TYPE_EXPENSE)) {
                    summary.expense += Math.abs(t.getAmount());
                } else {
                    summary.income += t.getAmount();
                }
            }

            // Check vượt ngân sách
            summary.isOverBudget = summary.expense > dailyBudget;
        }

        notifyDataSetChanged();
    }

    static class DayViewHolder extends RecyclerView.ViewHolder {
        private final ItemCalendarDayBinding binding;

        DayViewHolder(ItemCalendarDayBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}