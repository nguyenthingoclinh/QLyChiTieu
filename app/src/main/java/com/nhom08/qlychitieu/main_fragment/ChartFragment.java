package com.nhom08.qlychitieu.main_fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.nhom08.qlychitieu.MyApplication;
import com.nhom08.qlychitieu.R;
import com.nhom08.qlychitieu.adapter.ChartCategoryAdapter;
import com.nhom08.qlychitieu.chart.ChartManager;
import com.nhom08.qlychitieu.csdl.AppDatabase;
import com.nhom08.qlychitieu.databinding.FragmentChartBinding;
import com.nhom08.qlychitieu.dialog.MonthYearPickerDialog;
import com.nhom08.qlychitieu.mo_hinh.Category;
import com.nhom08.qlychitieu.mo_hinh.ChartCategoryInfo;
import com.nhom08.qlychitieu.mo_hinh.Transaction;
import com.nhom08.qlychitieu.mo_hinh.Transaction_DateRange;
import com.nhom08.qlychitieu.tien_ich.Constants;
import com.nhom08.qlychitieu.tien_ich.DateTimeUtils;
import com.nhom08.qlychitieu.tien_ich.MessageUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

/**
 * Fragment hiển thị thống kê chi tiêu/thu nhập theo biểu đồ
 * - Bao gồm biểu đồ tròn cho phân phối chi tiêu theo danh mục
 * - Biểu đồ cột cho chi tiêu theo ngày trong tháng
 * - Biểu đồ đường cho xu hướng chi tiêu/thu nhập qua các tháng
 * - Hiển thị RecyclerView chứa danh sách các danh mục chi tiêu/thu nhập
 * - Cho phép chuyển đổi giữa xem chi tiêu và thu nhập
 * - Hỗ trợ chọn tháng/năm để xem số liệu theo thời gian
 */
public class ChartFragment extends Fragment {
    private static final String TAG = "ChartFragment";

    private FragmentChartBinding binding;
    private MyApplication myApp;
    private AppDatabase database;
    private ExecutorService executorService;
    private MessageUtils messageUtils;
    private ChartManager chartManager;

    private Calendar currentCalendar;
    private boolean isExpenseTab = true; // Mặc định hiển thị tab chi tiêu
    private int currentChartType = 0; // 0: pie, 1: bar, 2: line

    private ChartCategoryAdapter categoryAdapter;
    private List<Category> allCategories = new ArrayList<>();
    private List<Transaction> transactions = new ArrayList<>();
    private List<ChartCategoryInfo> chartCategoryInfos = new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentChartBinding.inflate(inflater, container, false);
        init();
        setupViews();
        return binding.getRoot();
    }

    /**
     * Khởi tạo các thành phần cần thiết cho Fragment
     */
    private void init() {
        myApp = (MyApplication) requireActivity().getApplication();
        database = myApp.getDatabase();
        executorService = myApp.getExecutorService();
        messageUtils = new MessageUtils(requireContext());
        currentCalendar = Calendar.getInstance();

        categoryAdapter = new ChartCategoryAdapter(requireContext());

        // Khởi tạo ChartManager
        chartManager = new ChartManager(
                requireContext(),
                binding.pieChart,
                binding.barChart,
                binding.lineChart
        );

        // Thiết lập listener cho biểu đồ tròn
        chartManager.setPieChartListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                if (e instanceof PieEntry) {
                    PieEntry pe = (PieEntry) e;

                    // Lấy phần trăm của miếng được chọn
                    float percentage = pe.getValue() / getTotalValue() * 100;

                    // Hiển thị tên danh mục và phần trăm
                    String message = String.format(Locale.getDefault(), "%s: %.1f%%", pe.getLabel(), percentage);

                    // Hiển thị trong centerText của biểu đồ
                    binding.pieChart.setCenterText(message);
                }
            }

            @Override
            public void onNothingSelected() {
                // Khi không có gì được chọn, đặt lại text trung tâm
                String typeText = isExpenseTab ? "Chi tiêu" : "Thu nhập";
                binding.pieChart.setCenterText(typeText);
            }
        });
    }

    /**
     * Thiết lập các thành phần giao diện và xử lý sự kiện
     */
    private void setupViews() {
        // Setup RecyclerView hiển thị danh sách các danh mục
        binding.recyclerViewCategories.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerViewCategories.setAdapter(categoryAdapter);

        // Setup TabLayout để chuyển đổi giữa chi tiêu và thu nhập
        binding.tabLayout.addOnTabSelectedListener(new com.google.android.material.tabs.TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(com.google.android.material.tabs.TabLayout.Tab tab) {
                isExpenseTab = tab.getPosition() == 0; // Tab 0 là chi tiêu, tab 1 là thu nhập
                updateChartTitle(); // Cập nhật tiêu đề biểu đồ
                processTransactions(); // Xử lý lại dữ liệu để hiển thị
            }

            @Override
            public void onTabUnselected(com.google.android.material.tabs.TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(com.google.android.material.tabs.TabLayout.Tab tab) {}
        });

        // Setup RadioGroup để chọn loại biểu đồ
        setupChartTypeSelection();

        // Xử lý sự kiện điều hướng tháng trước/tháng sau
        binding.btnPrevMonth.setOnClickListener(v -> {
            currentCalendar.add(Calendar.MONTH, -1); // Lùi 1 tháng
            updateMonthYearDisplay(); // Cập nhật hiển thị tháng/năm
            observeTransactions(); // Tải lại dữ liệu giao dịch
        });
        binding.btnNextMonth.setOnClickListener(v -> {
            if (canNavigateToNextMonth()) { // Kiểm tra xem có thể tiến tới tháng sau không
                currentCalendar.add(Calendar.MONTH, 1); // Tiến 1 tháng
                updateMonthYearDisplay(); // Cập nhật hiển thị tháng/năm
                observeTransactions(); // Tải lại dữ liệu giao dịch
            }
        });
        binding.tvMonthYear.setOnClickListener(v -> showMonthYearPicker()); // Hiển thị dialog chọn tháng/năm

        updateMonthYearDisplay(); // Cập nhật hiển thị tháng/năm ban đầu
        observeTransactions(); // Tải dữ liệu giao dịch ban đầu
    }

    /**
     * Thiết lập xử lý sự kiện cho RadioGroup chuyển đổi loại biểu đồ
     */
    private void setupChartTypeSelection() {
        binding.chartTypeRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioPieChart) {
                currentChartType = 0;
                switchToChart(0);
            } else if (checkedId == R.id.radioBarChart) {
                currentChartType = 1;
                switchToChart(1);
            } else if (checkedId == R.id.radioLineChart) {
                currentChartType = 2;
                switchToChart(2);
            }
        });
    }

    /**
     * Hiển thị dialog chọn tháng/năm
     */
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

    /**
     * Cập nhật hiển thị tháng/năm và trạng thái các nút điều hướng
     */
    private void updateMonthYearDisplay() {
        Calendar now = Calendar.getInstance();
        binding.tvMonthYear.setText(DateTimeUtils.formatMonthYear(currentCalendar));

        // Cập nhật text của nút tháng trước
        Calendar prevMonth = (Calendar) currentCalendar.clone();
        prevMonth.add(Calendar.MONTH, -1);
        binding.btnPrevMonth.setText(String.format(Locale.getDefault(), "thg %d %d",
                prevMonth.get(Calendar.MONTH) + 1, prevMonth.get(Calendar.YEAR)));

        // Hiển thị/ẩn nút tháng sau
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

    /**
     * Kiểm tra xem có thể điều hướng tới tháng tiếp theo hay không
     */
    private boolean canNavigateToNextMonth() {
        Calendar now = Calendar.getInstance();
        return currentCalendar.get(Calendar.YEAR) < now.get(Calendar.YEAR) ||
                (currentCalendar.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                        currentCalendar.get(Calendar.MONTH) < now.get(Calendar.MONTH));
    }

    /**
     * Cập nhật tiêu đề biểu đồ dựa trên tab đang chọn
     */
    private void updateChartTitle() {
        String title;
        switch (currentChartType) {
            case 0:
                title = isExpenseTab ? "Biểu đồ phân bổ chi tiêu" : "Biểu đồ phân bổ thu nhập";
                break;
            case 1:
                title = isExpenseTab ? "Chi tiêu theo ngày" : "Thu nhập theo ngày";
                break;
            case 2:
                title = "Xu hướng 6 tháng gần nhất";
                break;
            default:
                title = "Biểu đồ thống kê";
        }
        binding.tvChartTitle.setText(title);
    }

    /**
     * Chuyển đổi hiển thị giữa các loại biểu đồ
     */
    private void switchToChart(int chartType) {
        switch (chartType) {
            case 0: // Biểu đồ tròn
                chartManager.showPieChart();
                updateChartTitle();
                updatePieChart();
                break;

            case 1: // Biểu đồ cột
                chartManager.showBarChart();
                updateChartTitle();
                updateBarChart();
                break;

            case 2: // Biểu đồ đường
                chartManager.showLineChart();
                updateChartTitle();
                loadMonthlyTrendData();
                break;
        }
    }

    /**
     * Quan sát dữ liệu giao dịch từ database
     */
    private void observeTransactions() {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.tvNoData.setVisibility(View.GONE);

        executorService.execute(() -> {
            try {
                int userId = myApp.getCurrentUserId();
                Transaction_DateRange dateRange = Transaction_DateRange.fromCalendar(currentCalendar);

                // Lấy danh sách phân loại
                allCategories = database.categoryDao().getAllCategories(userId);

                // Quan sát giao dịch theo khoảng thời gian với LiveData
                LiveData<List<Transaction>> liveTransactions = database.transactionDao()
                        .getTransactionsByDateRangeLive(
                                userId,
                                dateRange.getStartTime(),
                                dateRange.getEndTime()
                        );

                requireActivity().runOnUiThread(() -> {
                    // Quan sát LiveData để cập nhật khi dữ liệu thay đổi
                    liveTransactions.observe(getViewLifecycleOwner(), transactions -> {
                        this.transactions = transactions;
                        processTransactions();
                        binding.progressBar.setVisibility(View.GONE);
                    });
                });

            } catch (Exception e) {
                Log.e(TAG, "Lỗi khi tải dữ liệu giao dịch: " + e.getMessage());
                requireActivity().runOnUiThread(() -> {
                    binding.progressBar.setVisibility(View.GONE);
                    messageUtils.showError("Lỗi khi tải dữ liệu: " + e.getMessage());
                });
            }
        });
    }

    /**
     * Xử lý dữ liệu giao dịch để hiển thị thống kê
     */
    private void processTransactions() {
        if (!isAdded() || getActivity() == null) return;

        // Lọc giao dịch theo loại (chi tiêu hoặc thu nhập)
        String targetType = isExpenseTab ? Constants.CATEGORY_TYPE_EXPENSE : Constants.CATEGORY_TYPE_INCOME;

        // Lấy danh sách phân loại theo loại
        List<Category> filteredCategories = allCategories.stream()
                .filter(category -> targetType.equals(category.getType()))
                .collect(Collectors.toList());

        // Map để lưu trữ tổng số tiền theo phân loại
        Map<Integer, ChartCategoryInfo> categoryInfoMap = new HashMap<>();

        // Khởi tạo map với các phân loại
        for (int i = 0; i < filteredCategories.size(); i++) {
            Category category = filteredCategories.get(i);
            int colorIndex = i % ChartManager.CHART_COLORS.length;
            categoryInfoMap.put(category.getCategoryId(),
                    new ChartCategoryInfo(category, 0, ChartManager.CHART_COLORS[colorIndex]));
        }

        // Tính tổng số tiền cho mỗi phân loại
        double total = 0;
        for (Transaction transaction : transactions) {
            Category category = findCategoryById(transaction.getCategoryId());
            if (category != null && targetType.equals(category.getType())) {
                ChartCategoryInfo info = categoryInfoMap.get(category.getCategoryId());
                if (info != null) {
                    double amount = Math.abs(transaction.getAmount());
                    info.addAmount(amount);
                    total += amount;
                }
            }
        }

        // Chuyển map thành list và sắp xếp theo số tiền giảm dần
        chartCategoryInfos = categoryInfoMap.values().stream()
                .filter(info -> info.getAmount() > 0)
                .sorted((a, b) -> Double.compare(b.getAmount(), a.getAmount()))
                .collect(Collectors.toList());

        // Cập nhật adapter
        categoryAdapter.updateData(chartCategoryInfos, total);

        // Cập nhật tổng số tiền
        String typeText = isExpenseTab ? "chi tiêu" : "thu nhập";
        binding.tvTotal.setText(String.format(Locale.getDefault(),
                "Tổng %s: %,d đ", typeText, (long) total));
        binding.tvTotal.setTextColor(ContextCompat.getColor(requireContext(),
                isExpenseTab ? R.color.expense_color : R.color.income_color));

        // Kiểm tra xem có dữ liệu không
        if (chartCategoryInfos.isEmpty()) {
            binding.tvNoData.setVisibility(View.VISIBLE);
            binding.pieChart.setVisibility(View.GONE);
            binding.barChart.setVisibility(View.GONE);
            binding.lineChart.setVisibility(View.GONE);
        } else {
            binding.tvNoData.setVisibility(View.GONE);
            switchToChart(currentChartType);
        }
    }

    /**
     * Cập nhật biểu đồ tròn
     */
    private void updatePieChart() {
        if (chartCategoryInfos.isEmpty()) {
            binding.tvNoData.setVisibility(View.VISIBLE);
            binding.pieChart.setVisibility(View.GONE);
        } else {
            binding.tvNoData.setVisibility(View.GONE);
            binding.pieChart.setVisibility(View.VISIBLE);

            String typeText = isExpenseTab ? "Chi tiêu" : "Thu nhập";
            chartManager.updatePieChart(chartCategoryInfos, typeText);
        }
    }

    /**
     * Cập nhật biểu đồ cột
     */
    private void updateBarChart() {
        if (transactions.isEmpty()) {
            binding.tvNoData.setVisibility(View.VISIBLE);
            binding.barChart.setVisibility(View.GONE);
        } else {
            binding.tvNoData.setVisibility(View.GONE);
            binding.barChart.setVisibility(View.VISIBLE);

            chartManager.updateBarChart(
                    currentCalendar,
                    transactions,
                    isExpenseTab,
                    this::findCategoryById,
                    Constants.CATEGORY_TYPE_EXPENSE,
                    Constants.CATEGORY_TYPE_INCOME
            );
        }
    }

    /**
     * Tải dữ liệu xu hướng cho biểu đồ đường
     */
    private void loadMonthlyTrendData() {
        binding.progressBar.setVisibility(View.VISIBLE);

        executorService.execute(() -> {
            try {
                int userId = myApp.getCurrentUserId();

                // Lấy dữ liệu 6 tháng gần đây
                Calendar endDate = Calendar.getInstance();
                Calendar startDate = (Calendar) endDate.clone();
                startDate.add(Calendar.MONTH, -5);

                // Reset ngày về đầu tháng và cuối tháng
                startDate.set(Calendar.DAY_OF_MONTH, 1);
                startDate.set(Calendar.HOUR_OF_DAY, 0);
                startDate.set(Calendar.MINUTE, 0);
                startDate.set(Calendar.SECOND, 0);

                endDate.set(Calendar.DAY_OF_MONTH, endDate.getActualMaximum(Calendar.DAY_OF_MONTH));
                endDate.set(Calendar.HOUR_OF_DAY, 23);
                endDate.set(Calendar.MINUTE, 59);
                endDate.set(Calendar.SECOND, 59);

                // Lấy tất cả giao dịch trong khoảng thời gian
                List<Transaction> allTransactions = database.transactionDao().getTransactionsByDateRange(
                        userId, startDate.getTimeInMillis(), endDate.getTimeInMillis());

                // Map để lưu tổng theo tháng
                Map<String, Float> expenseByMonth = new HashMap<>();
                Map<String, Float> incomeByMonth = new HashMap<>();

                // Khởi tạo các tháng với giá trị 0
                List<String> monthLabels = new ArrayList<>();
                Calendar tempCalendar = (Calendar) startDate.clone();
                while (!tempCalendar.after(endDate)) {
                    String monthYear = DateTimeUtils.formatMonthYearShort(tempCalendar);
                    monthLabels.add(monthYear);
                    expenseByMonth.put(monthYear, 0f);
                    incomeByMonth.put(monthYear, 0f);
                    tempCalendar.add(Calendar.MONTH, 1);
                }

                // Tính tổng chi tiêu/thu nhập theo tháng
                for (Transaction transaction : allTransactions) {
                    Calendar transactionDate = Calendar.getInstance();
                    transactionDate.setTimeInMillis(transaction.getDate());
                    String monthYear = DateTimeUtils.formatMonthYearShort(transactionDate);

                    Category category = findCategoryById(transaction.getCategoryId());
                    if (category != null) {
                        float amount = (float) Math.abs(transaction.getAmount());
                        if (Constants.CATEGORY_TYPE_EXPENSE.equals(category.getType())) {

                            expenseByMonth.put(monthYear, expenseByMonth.getOrDefault(monthYear, 0f) + amount);
                        } else if (Constants.CATEGORY_TYPE_INCOME.equals(category.getType())) {
                            incomeByMonth.put(monthYear, incomeByMonth.getOrDefault(monthYear, 0f) + amount);
                        }
                    }
                }

                // Kết quả cuối cùng
                final List<String> finalMonthLabels = monthLabels;
                final Map<String, Float> finalExpenseByMonth = expenseByMonth;
                final Map<String, Float> finalIncomeByMonth = incomeByMonth;

                // Cập nhật UI
                requireActivity().runOnUiThread(() -> {
                    binding.tvNoData.setVisibility(View.GONE);
                    binding.lineChart.setVisibility(View.VISIBLE);
                    chartManager.updateLineChart(finalMonthLabels, finalExpenseByMonth, finalIncomeByMonth);
                    binding.progressBar.setVisibility(View.GONE);
                });

            } catch (Exception e) {
                Log.e(TAG, "Lỗi khi tải dữ liệu xu hướng: " + e.getMessage());
                requireActivity().runOnUiThread(() -> {
                    binding.progressBar.setVisibility(View.GONE);
                    messageUtils.showError("Lỗi khi tải dữ liệu xu hướng: " + e.getMessage());
                });
            }
        });
    }

    /**
     * Hàm trợ giúp để tính tổng giá trị của tất cả các miếng trong biểu đồ
     */
    private float getTotalValue() {
        float total = 0f;
        for (ChartCategoryInfo info : chartCategoryInfos) {
            total += (float) info.getAmount();
        }
        return total;
    }

    /**
     * Tìm phân loại theo ID
     */
    private Category findCategoryById(Integer categoryId) {
        if (categoryId == null) return null;
        return allCategories.stream()
                .filter(c -> c.getCategoryId() == categoryId)
                .findFirst()
                .orElse(null);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}