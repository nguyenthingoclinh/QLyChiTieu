package com.nhom08.qlychitieu.giao_dien.man_hinh_chinh;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.nhom08.qlychitieu.MyApplication;
import com.nhom08.qlychitieu.R;
import com.nhom08.qlychitieu.adapter.TransactionAdapter;
import com.nhom08.qlychitieu.csdl.AppDatabase;
import com.nhom08.qlychitieu.databinding.FragmentHomeBinding;
import com.nhom08.qlychitieu.dialog.MonthYearPickerDialog;
import com.nhom08.qlychitieu.mo_hinh.Category;
import com.nhom08.qlychitieu.mo_hinh.DailyTransaction;
import com.nhom08.qlychitieu.mo_hinh.Transaction;
import com.nhom08.qlychitieu.mo_hinh.Transaction_DateRange;
import com.nhom08.qlychitieu.tien_ich.Constants;
import com.nhom08.qlychitieu.tien_ich.DateTimeUtils;
import com.nhom08.qlychitieu.tien_ich.MessageUtils;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

public class HomeFragment extends Fragment {
    private static final String TAG = "HomeFragment";

    private FragmentHomeBinding binding;
    private MessageUtils messageUtils;
    private MyApplication myApp;
    private AppDatabase database;
    private ExecutorService executorService;
    private final List<Transaction> transactionList = new ArrayList<>();
    private final List<Transaction> originalTransactionList = new ArrayList<>();
    private TransactionAdapter transactionAdapter;
    private List<Category> categories = new ArrayList<>();
    private Calendar currentCalendar;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        init();
        setupViews();
        return binding.getRoot();
    }

    private void init() {
        myApp = (MyApplication) requireActivity().getApplication();
        database = myApp.getDatabase();
        executorService = myApp.getExecutorService();
        messageUtils = new MessageUtils(requireContext());
        currentCalendar = Calendar.getInstance();
    }

    private void setupViews() {
        // RecyclerView
        transactionAdapter = new TransactionAdapter(requireContext());
        binding.recyclerViewTransactions.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerViewTransactions.setAdapter(transactionAdapter);
        transactionAdapter.setOnTransactionDeleteListener(transaction -> {
            executorService.execute(() -> {
                myApp.getDatabase().transactionDao().deleteTransaction(transaction);
                // Room sẽ tự động update LiveData
            });
        });

        // Search
        binding.searchView.setOnQueryTextListener(new android.widget.SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                performSearch(query);
                return true;
            }
            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.isEmpty()) resetToOriginalList();
                else performSearch(newText);
                return true;
            }
        });

        // Navigation
        binding.btnPrevMonth.setOnClickListener(v -> {
            currentCalendar.add(Calendar.MONTH, -1);
            updateMonthYearDisplay();
            observeTransactions();
        });
        binding.btnNextMonth.setOnClickListener(v -> {
            if (canNavigateToNextMonth()) {
                currentCalendar.add(Calendar.MONTH, 1);
                updateMonthYearDisplay();
                observeTransactions();
            }
        });
        binding.btnCalendar.setOnClickListener(v -> showCalendarFragment());
        binding.tvMonthYear.setOnClickListener(v -> showMonthYearPicker());

        // Balance click
        View.OnClickListener balanceClickListener = v -> showTotalBalanceFragment();
        binding.tvExpense.setOnClickListener(balanceClickListener);
        binding.tvIncome.setOnClickListener(balanceClickListener);
        binding.tvBalance.setOnClickListener(balanceClickListener);

        updateMonthYearDisplay();
        observeTransactions();
    }

    /**
     * Tìm kiếm giao dịch theo từ khoá
     */
    private void performSearch(String keyword) {
        executorService.execute(() -> {
            try {
                int userId = myApp.getCurrentUserId();
                Transaction_DateRange dateRange = Transaction_DateRange.fromCalendar(currentCalendar);

                List<Transaction> searchResults = database.transactionDao()
                        .searchTransactions(userId, keyword)
                        .stream()
                        .filter(t -> dateRange.isInRange(t.getDate()))
                        .collect(Collectors.toList());

                updateTransactionListUI(searchResults);
            } catch (Exception e) {
                Log.e(TAG, "Lỗi tìm kiếm: " + e.getMessage());
                showError(R.string.error_search);
            }
        });
    }

    private void resetToOriginalList() {
        updateTransactionListUI(new ArrayList<>(originalTransactionList));
    }

    /**
     * Quan sát và cập nhật giao dịch theo tháng/năm đang chọn
     */
    private void observeTransactions() {
        executorService.execute(() -> {
            try {
                int userId = myApp.getCurrentUserId();
                Transaction_DateRange dateRange = Transaction_DateRange.fromCalendar(currentCalendar);
                categories = database.categoryDao().getAllCategories(userId);

                LiveData<List<Transaction>> liveTransactions = database.transactionDao()
                        .getTransactionsByDateRangeLive(
                                userId, dateRange.getStartTime(), dateRange.getEndTime()
                        );

                requireActivity().runOnUiThread(() -> {
                    liveTransactions.observe(getViewLifecycleOwner(), this::processTransactions);
                });

            } catch (Exception e) {
                Log.e(TAG, "Lỗi quan sát giao dịch: " + e.getMessage());
                showError(R.string.error_load_transactions);
            }
        });
    }

    /**
     * Xử lý giao dịch lấy về và cập nhật UI
     */
    private void processTransactions(List<Transaction> transactions) {
        if (!isAdded() || getActivity() == null || binding == null) return;

        double totalExpense = 0, totalIncome = 0;
        for (Transaction t : transactions) {
            Category c = findCategoryById(t.getCategoryId());
            if (c != null) {
                if (Constants.CATEGORY_TYPE_EXPENSE.equals(c.getType()))
                    totalExpense += Math.abs(t.getAmount());
                else
                    totalIncome += t.getAmount();
            }
        }
        updateMonthlyTotals(totalExpense, totalIncome);

        Map<Long, DailyTransaction> dailyMap = organizeTransactionsByDay(transactions);

        transactionAdapter.updateData(dailyMap.values(), categories);
        transactionList.clear();
        transactionList.addAll(transactions);
        originalTransactionList.clear();
        originalTransactionList.addAll(transactions);
    }

    /**
     * Cập nhật giao diện danh sách giao dịch khi tìm kiếm/reset
     */
    private void updateTransactionListUI(List<Transaction> transactions) {
        if (!isAdded() || getActivity() == null || binding == null) return;

        requireActivity().runOnUiThread(() -> {
            Map<Long, DailyTransaction> dailyMap = organizeTransactionsByDay(transactions);
            transactionAdapter.updateData(dailyMap.values(), categories);
            double totalExpense = 0, totalIncome = 0;
            for (Transaction t : transactions) {
                Category c = findCategoryById(t.getCategoryId());
                if (c != null) {
                    if (Constants.CATEGORY_TYPE_EXPENSE.equals(c.getType()))
                        totalExpense += Math.abs(t.getAmount());
                    else
                        totalIncome += t.getAmount();
                }
            }
            updateMonthlyTotals(totalExpense, totalIncome);
        });
    }

    /**
     * Gom nhóm giao dịch theo ngày
     */
    private Map<Long, DailyTransaction> organizeTransactionsByDay(List<Transaction> transactions) {
        // Sử dụng TreeMap với Comparator đảo ngược để ngày mới nhất lên đầu
        Map<Long, DailyTransaction> dailyMap = new TreeMap<>(Collections.reverseOrder());

        for (Transaction transaction : transactions) {
            long dateKey = DateTimeUtils.getDayStartTimestamp(transaction.getDate());
            DailyTransaction daily = dailyMap.computeIfAbsent(dateKey, DailyTransaction::new);
            daily.addTransaction(transaction, categories);
        }

        // Đảm bảo các transaction trong mỗi ngày cũng được sắp xếp giảm dần theo thời gian
        for (DailyTransaction daily : dailyMap.values()) {
            daily.getTransactions().sort((t1, t2) -> Long.compare(t2.getDate(), t1.getDate()));
        }

        return dailyMap;
    }

    private void updateMonthlyTotals(double totalExpense, double totalIncome) {
        if (!isAdded() || getActivity() == null || binding == null) return;
        requireActivity().runOnUiThread(() -> {
            binding.tvExpense.setText(String.format(Locale.getDefault(), "%,d", (long) totalExpense));
            binding.tvIncome.setText(String.format(Locale.getDefault(), "%,d", (long) totalIncome));
            binding.tvBalance.setText(String.format(Locale.getDefault(), "%,d", (long) (totalIncome - totalExpense)));
        });
    }

    /**
     * Hiển thị tháng/năm ở header và cập nhật trạng thái nút tháng trước/sau
     */
    private void updateMonthYearDisplay() {
        Calendar now = Calendar.getInstance();
        binding.tvMonthYear.setText(DateTimeUtils.formatMonthYear(currentCalendar));
        binding.tvYear.setText(String.valueOf(currentCalendar.get(Calendar.YEAR)));

        // Nút tháng trước
        Calendar prevMonth = (Calendar) currentCalendar.clone();
        prevMonth.add(Calendar.MONTH, -1);
        binding.btnPrevMonth.setText(String.format(Locale.getDefault(), "thg %d %d",
                prevMonth.get(Calendar.MONTH) + 1, prevMonth.get(Calendar.YEAR)));

        // Nút tháng sau
        boolean isCurrentMonth = currentCalendar.get(Calendar.YEAR) == now.get(Calendar.YEAR)
                && currentCalendar.get(Calendar.MONTH) == now.get(Calendar.MONTH);
        if (!isCurrentMonth) {
            Calendar nextMonth = (Calendar) currentCalendar.clone();
            nextMonth.add(Calendar.MONTH, 1);
            binding.btnNextMonth.setText(String.format(Locale.getDefault(), "thg %d %d",
                    nextMonth.get(Calendar.MONTH) + 1, nextMonth.get(Calendar.YEAR)));
            binding.btnNextMonth.setVisibility(View.VISIBLE);
        } else {
            binding.btnNextMonth.setVisibility(View.GONE);
        }
    }

    private boolean canNavigateToNextMonth() {
        Calendar now = Calendar.getInstance();
        return currentCalendar.get(Calendar.YEAR) < now.get(Calendar.YEAR) ||
                (currentCalendar.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                        currentCalendar.get(Calendar.MONTH) < now.get(Calendar.MONTH));
    }

    private void showMonthYearPicker() {
        MonthYearPickerDialog dialog = new MonthYearPickerDialog(
                currentCalendar,
                (year, month) -> {
                    currentCalendar.set(Calendar.YEAR, year);
                    currentCalendar.set(Calendar.MONTH, month);
                    currentCalendar.set(Calendar.DAY_OF_MONTH, 1);
                    updateMonthYearDisplay();
                    observeTransactions();
                });
        dialog.show(getParentFragmentManager(), "MonthYearPickerDialog");
    }

    private void showCalendarFragment() {
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.frame_layout, new CalendarFragment())
                .addToBackStack(null)
                .commit();
    }

    private void showTotalBalanceFragment() {
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.frame_layout, new TotalBalanceFragment())
                .addToBackStack(null)
                .commit();
    }

    private Category findCategoryById(int categoryId) {
        for (Category c : categories) {
            if (c.getCategoryId() == categoryId) return c;
        }
        return null;
    }

    private void showError(int messageId) {
        if (!isAdded() || getActivity() == null) return;
        requireActivity().runOnUiThread(() -> messageUtils.showError(messageId));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}