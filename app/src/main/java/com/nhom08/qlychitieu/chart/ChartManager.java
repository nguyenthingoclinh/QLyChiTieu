package com.nhom08.qlychitieu.chart;

import android.content.Context;
import android.graphics.Color;
import android.view.View;

import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.nhom08.qlychitieu.R;
import com.nhom08.qlychitieu.mo_hinh.Category;
import com.nhom08.qlychitieu.mo_hinh.ChartCategoryInfo;
import com.nhom08.qlychitieu.mo_hinh.Transaction;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Class quản lý các biểu đồ trong ứng dụng
 * - Chịu trách nhiệm thiết lập và cập nhật các loại biểu đồ khác nhau
 * - Giúp giảm kích thước của Fragment và tăng khả năng tái sử dụng
 */
public class ChartManager {
    private final Context context;

    // Các biểu đồ
    private final PieChart pieChart;
    private final BarChart barChart;
    private final LineChart lineChart;

    // Màu sắc cho biểu đồ
    public static int[] CHART_COLORS = {
    };

    /**
     * Constructor
     * @param context Context của ứng dụng
     * @param pieChart Biểu đồ tròn
     * @param barChart Biểu đồ cột
     * @param lineChart Biểu đồ đường
     */
    public ChartManager(Context context, PieChart pieChart, BarChart barChart, LineChart lineChart) {
        this.context = context;
        this.pieChart = pieChart;
        this.barChart = barChart;
        this.lineChart = lineChart;

        // Khởi tạo mảng màu từ resources

        // Gán lại CHART_COLORS sau khi
        CHART_COLORS = new int[] {
                ContextCompat.getColor(context, R.color.chart_color_1),
                ContextCompat.getColor(context, R.color.chart_color_2),
                ContextCompat.getColor(context, R.color.chart_color_3),
                ContextCompat.getColor(context, R.color.chart_color_4),
                ContextCompat.getColor(context, R.color.chart_color_5),
                ContextCompat.getColor(context, R.color.chart_color_6),
                ContextCompat.getColor(context, R.color.chart_color_7),
                ContextCompat.getColor(context, R.color.chart_color_8),
                ContextCompat.getColor(context, R.color.chart_color_9),
                ContextCompat.getColor(context, R.color.chart_color_10),
                ContextCompat.getColor(context, R.color.chart_color_11),
                ContextCompat.getColor(context, R.color.chart_color_12),
                ContextCompat.getColor(context, R.color.chart_color_13),
                ContextCompat.getColor(context, R.color.chart_color_14),
                ContextCompat.getColor(context, R.color.chart_color_15)
        };

        setupCharts();
    }

    /**
     * Thiết lập cấu hình ban đầu cho tất cả các biểu đồ
     */
    private void setupCharts() {
        setupPieChart();
        setupBarChart();
        setupLineChart();
    }

    // ================= BIỂU ĐỒ TRÒN =================

    /**
     * Thiết lập cấu hình cho biểu đồ tròn
     */
    private void setupPieChart() {
        pieChart.setUsePercentValues(true);
        pieChart.getDescription().setEnabled(false);
        pieChart.setExtraOffsets(5, 10, 5, 5);
        pieChart.setDragDecelerationFrictionCoef(0.95f);

        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(Color.WHITE);
        pieChart.setTransparentCircleColor(Color.WHITE);
        pieChart.setTransparentCircleAlpha(110);
        pieChart.setHoleRadius(58f);
        pieChart.setTransparentCircleRadius(61f);

        pieChart.setDrawCenterText(true);
        pieChart.setRotationAngle(0);
        pieChart.setRotationEnabled(true);
        pieChart.setHighlightPerTapEnabled(true);

        // Tắt chú thích
        Legend l = pieChart.getLegend();
        l.setEnabled(false);

        // Tắt hiển thị nhãn
        pieChart.setDrawEntryLabels(false);
    }

    /**
     * Thiết lập listener cho biểu đồ tròn
     * @param onValueSelectedListener Listener xử lý khi chọn giá trị trên biểu đồ
     */
    public void setPieChartListener(OnChartValueSelectedListener onValueSelectedListener) {
        pieChart.setOnChartValueSelectedListener(onValueSelectedListener);
    }

    /**
     * Cập nhật dữ liệu biểu đồ tròn
     * @param chartCategoryInfos Danh sách thông tin danh mục
     * @param centerText Văn bản hiển thị giữa biểu đồ
     */
    public void updatePieChart(List<ChartCategoryInfo> chartCategoryInfos, String centerText) {
        ArrayList<PieEntry> entries = new ArrayList<>();
        ArrayList<Integer> colors = new ArrayList<>();

        for (ChartCategoryInfo info : chartCategoryInfos) {
            entries.add(new PieEntry((float) info.getAmount(), info.getCategory().getName()));
            colors.add(info.getColor());
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(colors);
        dataSet.setSliceSpace(0f);
        dataSet.setSelectionShift(10f);

        PieData data = new PieData(dataSet);
        data.setValueFormatter(new PercentFormatter(pieChart));
        data.setDrawValues(false);

        pieChart.setData(data);
        pieChart.setCenterText(centerText);
        pieChart.invalidate();
        pieChart.animateY(1400, Easing.EaseInOutQuad);
    }

    /**
     * Hiển thị biểu đồ tròn
     */
    public void showPieChart() {
        pieChart.setVisibility(View.VISIBLE);
        barChart.setVisibility(View.GONE);
        lineChart.setVisibility(View.GONE);
    }

    // ================= BIỂU ĐỒ CỘT =================

    /**
     * Thiết lập cấu hình cho biểu đồ cột
     */
    private void setupBarChart() {
        barChart.getDescription().setEnabled(false);
        barChart.setMaxVisibleValueCount(31);
        barChart.setPinchZoom(false);
        barChart.setDrawBarShadow(false);
        barChart.setDrawGridBackground(false);
        barChart.setDrawValueAboveBar(true);
        barChart.setDoubleTapToZoomEnabled(true);

        // Cấu hình trục X
        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setLabelCount(7);
        xAxis.setTextSize(11f);
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.valueOf((int)value);
            }
        });

        // Cấu hình trục Y bên trái
        YAxis leftAxis = barChart.getAxisLeft();
        leftAxis.setLabelCount(8, false);
        leftAxis.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART);
        leftAxis.setSpaceTop(15f);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setTextSize(11f);
        leftAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                if (value > 1000000) {
                    return String.format(Locale.getDefault(), "%.1ftr", value / 1000000);
                } else if (value > 1000) {
                    return String.format(Locale.getDefault(), "%.0fk", value / 1000);
                } else {
                    return String.valueOf((int)value);
                }
            }
        });

        barChart.getAxisRight().setEnabled(false);

        Legend l = barChart.getLegend();
        l.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        l.setHorizontalAlignment(Legend.LegendHorizontalAlignment.LEFT);
        l.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        l.setDrawInside(false);
        l.setForm(Legend.LegendForm.SQUARE);
        l.setFormSize(9f);
        l.setTextSize(11f);
        l.setXEntrySpace(4f);
    }

    /**
     * Cập nhật dữ liệu biểu đồ cột
     * @param calendar Calendar hiện tại để xác định số ngày trong tháng
     * @param transactions Danh sách giao dịch
     * @param isExpenseTab Đang xem chi tiêu (true) hay thu nhập (false)
     * @param findCategoryById Hàm lambda để tìm category theo ID
     * @param categoryTypeExpense Hằng số loại category chi tiêu
     * @param categoryTypeIncome Hằng số loại category thu nhập
     */
    public void updateBarChart(
            Calendar calendar,
            List<Transaction> transactions,
            boolean isExpenseTab,
            FindCategoryById findCategoryById,
            String categoryTypeExpense,
            String categoryTypeIncome) {

        Map<Integer, Float> dailyAmounts = new HashMap<>();
        String targetType = isExpenseTab ? categoryTypeExpense : categoryTypeIncome;
        int daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);

        // Khởi tạo tất cả các ngày với giá trị 0
        for (int day = 1; day <= daysInMonth; day++) {
            dailyAmounts.put(day, 0f);
        }

        // Tính tổng số tiền cho mỗi ngày
        for (Transaction t : transactions) {
            Category category = (Category) findCategoryById.find(t.getCategoryId());
            if (category != null && targetType.equals(category.getType())) {
                Calendar transactionDate = Calendar.getInstance();
                transactionDate.setTimeInMillis(t.getDate());
                int dayOfMonth = transactionDate.get(Calendar.DAY_OF_MONTH);

                float amount = (float) Math.abs(t.getAmount());
                dailyAmounts.put(dayOfMonth, dailyAmounts.getOrDefault(dayOfMonth, 0f) + amount);
            }
        }

        // Tạo entries cho biểu đồ
        ArrayList<BarEntry> entries = new ArrayList<>();
        for (int day = 1; day <= daysInMonth; day++) {
            entries.add(new BarEntry(day, dailyAmounts.get(day)));
        }

        // Tạo dataset với màu sắc phù hợp
        BarDataSet dataSet;
        if (barChart.getData() != null && barChart.getData().getDataSetCount() > 0) {
            dataSet = (BarDataSet) barChart.getData().getDataSetByIndex(0);
            dataSet.setValues(entries);

            // Cập nhật lại nhãn và màu sắc phù hợp khi chuyển tab
            dataSet.setLabel(isExpenseTab ? "Chi tiêu theo ngày" : "Thu nhập theo ngày");
            dataSet.setColor(ContextCompat.getColor(context,
                    isExpenseTab ? R.color.expense_color : R.color.income_color));

            // Cập nhật màu gradient
            try {
                dataSet.setGradientColor(
                        ContextCompat.getColor(context,
                                isExpenseTab ? R.color.expense_color_light : R.color.income_color_light),
                        ContextCompat.getColor(context,
                                isExpenseTab ? R.color.expense_color : R.color.income_color)
                );
            } catch (Exception e) {
                // Không có màu gradient đã định nghĩa
            }
            barChart.getData().notifyDataChanged();
            barChart.notifyDataSetChanged();
        } else {
            dataSet = new BarDataSet(entries, isExpenseTab ? "Chi tiêu theo ngày" : "Thu nhập theo ngày");
            dataSet.setColor(ContextCompat.getColor(context,
                    isExpenseTab ? R.color.expense_color : R.color.income_color));

            // Có thể thêm hiệu ứng gradient cho đẹp nếu màu đã định nghĩa
            try {
                dataSet.setGradientColor(
                        ContextCompat.getColor(context,
                                isExpenseTab ? R.color.expense_color_light : R.color.income_color_light),
                        ContextCompat.getColor(context,
                                isExpenseTab ? R.color.expense_color : R.color.income_color)
                );
            } catch (Exception e) {
                // Không có màu gradient đã định nghĩa
            }

            dataSet.setDrawValues(true);
            dataSet.setValueTextSize(10f);
            dataSet.setValueFormatter(new ValueFormatter() {
                @Override
                public String getFormattedValue(float value) {
                    if (value == 0) return "";
                    if (value > 1000000) {
                        return String.format(Locale.getDefault(), "%.1ftr", value / 1000000);
                    } else if (value > 1000) {
                        return String.format(Locale.getDefault(), "%.0fk", value / 1000);
                    } else {
                        return String.valueOf((int)value);
                    }
                }
            });

            BarData data = new BarData(dataSet);
            data.setBarWidth(0.8f);
            barChart.setData(data);
        }

        // Sửa đổi để có thể thu phóng
        barChart.setScaleEnabled(true);
        barChart.setPinchZoom(true);

        // Điều chỉnh trục Y bên trái để giá trị hiển thị lớn hơn
        YAxis leftAxis = barChart.getAxisLeft();
        leftAxis.setTextSize(11f); // Tăng kích thước chữ
        leftAxis.setTextColor(ContextCompat.getColor(context,
                isExpenseTab ? R.color.expense_color_dark : R.color.income_color_dark));

        barChart.setFitBars(true);
        barChart.invalidate();
        barChart.animateY(1000);
    }

    /**
     * Hiển thị biểu đồ cột
     */
    public void showBarChart() {
        pieChart.setVisibility(View.GONE);
        barChart.setVisibility(View.VISIBLE);
        lineChart.setVisibility(View.GONE);
    }

    // ================= BIỂU ĐỒ ĐƯỜNG =================

    /**
     * Thiết lập cấu hình cho biểu đồ đường
     */
    private void setupLineChart() {
        lineChart.getDescription().setEnabled(false);
        lineChart.setTouchEnabled(true);
        lineChart.setDrawGridBackground(false);
        lineChart.setDragEnabled(true);
        lineChart.setScaleEnabled(true);
        lineChart.setPinchZoom(true);
        lineChart.setExtraOffsets(10, 10, 10, 10);

        // Cấu hình trục X
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);
        xAxis.setLabelRotationAngle(-45);

        // Cấu hình trục Y bên trái
        YAxis leftAxis = lineChart.getAxisLeft();
        leftAxis.setLabelCount(6, false);
        leftAxis.setDrawZeroLine(false);
        leftAxis.setDrawGridLines(true);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                if (value > 1000000) {
                    return String.format(Locale.getDefault(), "%.1ftr", value / 1000000);
                } else if (value > 1000) {
                    return String.format(Locale.getDefault(), "%.0fk", value / 1000);
                } else {
                    return String.valueOf((int)value);
                }
            }
        });

        lineChart.getAxisRight().setEnabled(false);

        Legend l = lineChart.getLegend();
        l.setEnabled(true);
        l.setForm(Legend.LegendForm.LINE);
        l.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        l.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        l.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        l.setDrawInside(false);
    }

    /**
     * Cập nhật biểu đồ đường với dữ liệu xu hướng
     * @param months Danh sách các tháng
     * @param expenseByMonth Map chứa dữ liệu chi tiêu theo tháng
     * @param incomeByMonth Map chứa dữ liệu thu nhập theo tháng
     */
    public void updateLineChart(List<String> months, Map<String, Float> expenseByMonth, Map<String, Float> incomeByMonth) {
        // Kiểm tra dữ liệu
        if (months == null || months.isEmpty() || expenseByMonth == null || incomeByMonth == null) {
            lineChart.setNoDataText("Không có dữ liệu để hiển thị");
            lineChart.invalidate();
            return;
        }

        ArrayList<Entry> expenseEntries = new ArrayList<>();
        ArrayList<Entry> incomeEntries = new ArrayList<>();

        // Tạo entries cho từng loại dữ liệu
        for (int i = 0; i < months.size(); i++) {
            String month = months.get(i);
            Float expenseAmount = expenseByMonth.getOrDefault(month, 0f);
            Float incomeAmount = incomeByMonth.getOrDefault(month, 0f);

            expenseEntries.add(new Entry(i, expenseAmount));
            incomeEntries.add(new Entry(i, incomeAmount));
        }

        // Dataset chi tiêu
        LineDataSet expenseDataSet = createLineDataSet(
                expenseEntries,
                "Chi tiêu",
                R.color.expense_color,
                R.color.expense_color_light);

        // Dataset thu nhập
        LineDataSet incomeDataSet = createLineDataSet(
                incomeEntries,
                "Thu nhập",
                R.color.income_color,
                R.color.income_color_light);

        // Tạo LineData từ các dataset
        LineData lineData = new LineData(expenseDataSet, incomeDataSet);

        // Thiết lập định dạng cho nhãn trục X
        lineChart.getXAxis().setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int index = (int) value;
                if (index >= 0 && index < months.size()) {
                    return months.get(index);
                }
                return "";
            }
        });

        // Thiết lập dữ liệu cho marker khi người dùng chạm vào biểu đồ
        CustomMarkerView marker = new CustomMarkerView(context, R.layout.marker_view, months);
        marker.setChartView(lineChart);
        lineChart.setMarker(marker);

        // Đặt dữ liệu vào biểu đồ
        lineChart.setData(lineData);
        lineChart.invalidate();

        // Thêm animation
        lineChart.animateXY(1500, 1500);

        // Đảm bảo thấy hết dữ liệu
        lineChart.setVisibleXRangeMaximum(6);
        lineChart.moveViewToX(0);
    }

    /**
     * Tạo LineDataSet với các thuộc tính đã được định nghĩa
     */
    private LineDataSet createLineDataSet(ArrayList<Entry> entries, String label, int colorResId, int fillColorResId) {
        LineDataSet dataSet = new LineDataSet(entries, label);

        // Đặt thuộc tính cho đường
        dataSet.setColor(ContextCompat.getColor(context, colorResId));
        dataSet.setLineWidth(2.5f);

        // Đặt thuộc tính cho điểm
        dataSet.setCircleColor(ContextCompat.getColor(context, colorResId));
        dataSet.setCircleRadius(5f);
        dataSet.setDrawCircleHole(true);
        dataSet.setCircleHoleRadius(2.5f);
        dataSet.setCircleHoleColor(Color.WHITE);

        // Đặt thuộc tính cho highlight khi chạm
        dataSet.setHighlightEnabled(true);
        dataSet.setHighLightColor(ContextCompat.getColor(context, colorResId));
        dataSet.setHighlightLineWidth(1.5f);

        // Tạo hiệu ứng đường cong và tô màu bên dưới đường
        dataSet.setDrawFilled(true);
        dataSet.setFillAlpha(50);
        try {
            dataSet.setFillColor(ContextCompat.getColor(context, fillColorResId));
        } catch (Exception e) {
            dataSet.setFillColor(ContextCompat.getColor(context, colorResId));
        }
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);

        // Thiết lập hiển thị giá trị
        dataSet.setDrawValues(true);
        dataSet.setValueTextSize(9f);
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                if (value < 1000) return "";
                if (value > 1000000) {
                    return String.format(Locale.getDefault(), "%.1ftr", value / 1000000);
                } else if (value > 1000) {
                    return String.format(Locale.getDefault(), "%.0fk", value / 1000);
                }
                return "";
            }
        });

        return dataSet;
    }

    /**
     * Hiển thị biểu đồ đường
     */
    public void showLineChart() {
        pieChart.setVisibility(View.GONE);
        barChart.setVisibility(View.GONE);
        lineChart.setVisibility(View.VISIBLE);
    }

    /**
     * Interface để tìm category theo ID
     */
    public interface FindCategoryById {
        Object find(Integer categoryId);
    }
}