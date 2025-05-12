package com.nhom08.qlychitieu.giao_dien.man_hinh_chinh;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.nhom08.qlychitieu.MyApplication;
import com.nhom08.qlychitieu.adapter.CalendarDayAdapter;
import com.nhom08.qlychitieu.adapter.TransactionAdapter;
import com.nhom08.qlychitieu.csdl.AppDatabase;
import com.nhom08.qlychitieu.databinding.DialogDayTransactionsBinding;
import com.nhom08.qlychitieu.databinding.FragmentCalendarBinding;
import com.nhom08.qlychitieu.dialog.MonthYearPickerDialog;
import com.nhom08.qlychitieu.mo_hinh.Category;
import com.nhom08.qlychitieu.mo_hinh.DailyTransaction;
import com.nhom08.qlychitieu.mo_hinh.Transaction;
import com.nhom08.qlychitieu.tien_ich.Constants;
import com.nhom08.qlychitieu.tien_ich.DateTimeUtils;
import com.nhom08.qlychitieu.tien_ich.MessageUtils;

import java.text.NumberFormat;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;

public class CalendarFragment extends Fragment implements CalendarDayAdapter.OnDayClickListener {

    private FragmentCalendarBinding binding;
    private CalendarDayAdapter calendarDayAdapter;
    private MessageUtils messageUtils;
    private MyApplication myApp;
    private AppDatabase database;
    private ExecutorService executorService;

    private List<Category> categories = new ArrayList<>();
    private Calendar selectedCalendar = Calendar.getInstance();
    private int selectedDay = -1;
    private int selectedMonth;
    private int selectedYear;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentCalendarBinding.inflate(inflater, container, false);
        initGlobals();
        setupCalendarGrid();
        setupMonthPicker();
        setupToolbar();

        // Mặc định hiện tháng hiện tại
        selectedMonth = selectedCalendar.get(Calendar.MONTH);
        selectedYear = selectedCalendar.get(Calendar.YEAR);

        loadCategoriesAndCalendar(selectedYear, selectedMonth);

        return binding.getRoot();
    }
    private void setupToolbar() {
        binding.btnBack.setOnClickListener(v -> {;
            requireActivity().onBackPressed();
        });
    }

    private void initGlobals() {
        myApp = (MyApplication) requireActivity().getApplication();
        database = myApp.getDatabase();
        executorService = myApp.getExecutorService();
        messageUtils = new MessageUtils(requireContext());
    }

    private void setupCalendarGrid() {
        calendarDayAdapter = new CalendarDayAdapter(requireContext(), this);
        binding.recyclerViewCalendar.setLayoutManager(new GridLayoutManager(getContext(), 7));
        binding.recyclerViewCalendar.setAdapter(calendarDayAdapter);
    }

    private void setupMonthPicker() {
        binding.spinnerMonth.setOnClickListener(v -> showMonthYearPicker());
    }

    private void showMonthYearPicker() {
        MonthYearPickerDialog dialog = new MonthYearPickerDialog(
                selectedCalendar,
                (year, month) -> {
                    selectedYear = year;
                    selectedMonth = month;
                    selectedCalendar.set(Calendar.YEAR, year);
                    selectedCalendar.set(Calendar.MONTH, month);
                    selectedCalendar.set(Calendar.DAY_OF_MONTH, 1);
                    loadCategoriesAndCalendar(year, month);
                });
        dialog.show(getParentFragmentManager(), "MonthYearPickerDialog");
    }

    private void loadCategoriesAndCalendar(int year, int month) {
        executorService.execute(() -> {
            try {
                int userId = myApp.getCurrentUserId();
                categories = database.categoryDao().getAllCategories(userId);

                // Lấy giao dịch trong tháng để hiển thị calendar
                Calendar monthStart = Calendar.getInstance();
                monthStart.set(year, month, 1, 0, 0, 0);
                monthStart.set(Calendar.MILLISECOND, 0);

                Calendar monthEnd = (Calendar) monthStart.clone();
                monthEnd.set(Calendar.DAY_OF_MONTH, monthStart.getActualMaximum(Calendar.DAY_OF_MONTH));
                monthEnd.set(Calendar.HOUR_OF_DAY, 23);
                monthEnd.set(Calendar.MINUTE, 59);
                monthEnd.set(Calendar.SECOND, 59);
                monthEnd.set(Calendar.MILLISECOND, 999);

                // Sử dụng LiveData
                LiveData<List<Transaction>> monthTransactionsLiveData = database.transactionDao()
                        .getTransactionsByDateRangeLive(
                                userId, monthStart.getTimeInMillis(), monthEnd.getTimeInMillis()
                        );

                // Observe LiveData trên main thread
                requireActivity().runOnUiThread(() -> {
                    updateCalendarHeader(year, month);

                    // Observe changes
                    monthTransactionsLiveData.observe(getViewLifecycleOwner(), transactions -> {
                        if (transactions != null) {
                            double monthlyBudget = calculateMonthlyIncome(transactions);
                            updateAverageBudget(monthlyBudget);
                            calendarDayAdapter.setMonth(year, month, transactions, categories, monthlyBudget);
                        }
                    });
                });

            } catch (Exception e) {
                requireActivity().runOnUiThread(() ->
                        messageUtils.showError("Không thể tải dữ liệu lịch: " + e.getMessage())
                );
            }
        });
    }

//    Tính ngân sách tháng từ giao dịch
    private double calculateMonthlyIncome(List<Transaction> transactions) {
        double monthlyBudget = 0;
        for (Transaction t : transactions) {
            Category cat = categories.stream()
                    .filter(c -> c.getCategoryId() == t.getCategoryId())
                    .findFirst()
                    .orElse(null);

            if (cat != null && cat.getType().equals(Constants.CATEGORY_TYPE_INCOME)) {
                monthlyBudget += t.getAmount();
            }
        }
        return monthlyBudget;
    }

    private void updateCalendarHeader(int year, int month) {
        String monthText = String.format(Locale.getDefault(), "thg %d %d", month + 1, year);
        binding.spinnerMonth.setText(monthText);
    }

    private void updateAverageBudget(double monthlyBudget) {
        int daysInMonth = selectedCalendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        double dailyBudget = monthlyBudget / daysInMonth;

        NumberFormat nf = NumberFormat.getInstance(new Locale("vi", "VN"));
        binding.tvAverageBudget.setText(
                "Ngân sách trung bình hằng ngày: " + nf.format(dailyBudget)
        );
    }

    @Override
    public void onDayClick(int day, int year, int month) {
        selectedDay = day;
        selectedYear = year;
        selectedMonth = month;
        selectedCalendar.set(Calendar.YEAR, year);
        selectedCalendar.set(Calendar.MONTH, month);
        selectedCalendar.set(Calendar.DAY_OF_MONTH, day);

        showDayTransactions();
    }

    private void showDayTransactions() {
        Dialog dialog = new Dialog(requireContext());
        DialogDayTransactionsBinding dialogBinding = DialogDayTransactionsBinding.inflate(getLayoutInflater());
        dialog.setContentView(dialogBinding.getRoot());

        // Set window width và background
        Window window = dialog.getWindow();
        if (window != null) {
            DisplayMetrics metrics = new DisplayMetrics();
            window.getWindowManager().getDefaultDisplay().getMetrics(metrics);
            int width = (int) (metrics.widthPixels * 0.9);
            window.setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT);
            window.setBackgroundDrawable(new ColorDrawable(Color.WHITE));
        }

        // Format và hiển thị ngày
        String dateText = selectedDay + " thg " + (selectedMonth + 1) + " " + selectedYear;
        dialogBinding.tvDateHeader.setText(dateText);

        // Setup RecyclerView cho transactions
        TransactionAdapter adapter = new TransactionAdapter(requireContext());
        dialogBinding.recyclerViewTransactions.setLayoutManager(
                new LinearLayoutManager(requireContext())
        );
        dialogBinding.recyclerViewTransactions.setAdapter(adapter);

        // Load transactions
        loadDayTransactions(adapter);

        dialog.show();
    }

    private void loadDayTransactions(TransactionAdapter adapter) {
        Calendar dayStart = (Calendar) selectedCalendar.clone();
        dayStart.set(Calendar.HOUR_OF_DAY, 0);
        dayStart.set(Calendar.MINUTE, 0);
        dayStart.set(Calendar.SECOND, 0);
        dayStart.set(Calendar.MILLISECOND, 0);

        Calendar dayEnd = (Calendar) dayStart.clone();
        dayEnd.set(Calendar.HOUR_OF_DAY, 23);
        dayEnd.set(Calendar.MINUTE, 59);
        dayEnd.set(Calendar.SECOND, 59);
        dayEnd.set(Calendar.MILLISECOND, 999);

        executorService.execute(() -> {
            List<Transaction> transactions = database.transactionDao()
                    .getTransactionsByDateRange(
                            myApp.getCurrentUserId(),
                            dayStart.getTimeInMillis(),
                            dayEnd.getTimeInMillis()
                    );

            requireActivity().runOnUiThread(() -> {
                if (!transactions.isEmpty()) {
                    // Dùng lại logic từ HomeFragment
                    Map<Long, DailyTransaction> dailyMap = new TreeMap<>(Collections.reverseOrder());
                    DailyTransaction daily = new DailyTransaction(dayStart.getTimeInMillis());

                    for (Transaction t : transactions) {
                        daily.addTransaction(t, categories);
                    }

                    dailyMap.put(dayStart.getTimeInMillis(), daily);
                    adapter.updateData(dailyMap.values(), categories);
                }
            });
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}