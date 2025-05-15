package com.nhom08.qlychitieu.chart;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;

import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.utils.MPPointF;
import com.nhom08.qlychitieu.R;
import com.nhom08.qlychitieu.databinding.MarkerViewBinding;

import java.util.List;
import java.util.Locale;

/**
 * Marker tùy chỉnh để hiển thị khi người dùng chạm vào biểu đồ
 */
public class CustomMarkerView extends MarkerView {
    private MarkerViewBinding binding;
    private List<String> monthLabels;

    /**
     * Constructor với Context
     */
    public CustomMarkerView(Context context) {
        super(context, R.layout.marker_view);
        initBinding(context);
    }

    /**
     * Constructor với Context và AttributeSet
     */
    public CustomMarkerView(Context context, AttributeSet attrs) {
        super(context, R.layout.marker_view);
        initBinding(context);
    }

    /**
     * Constructor với Context, AttributeSet, và int
     * Lưu ý: MarkerView chỉ nhận 2 tham số (context, layoutResource)
     */
    public CustomMarkerView(Context context, AttributeSet attrs, int defStyle) {
        // Chỉ truyền context và layout resource cho super
        super(context, R.layout.marker_view);
        initBinding(context);
    }

    /**
     * Constructor chính được sử dụng
     */
    public CustomMarkerView(Context context, int layoutResource, List<String> monthLabels) {
        super(context, layoutResource);
        this.monthLabels = monthLabels;
        initBinding(context);
    }

    /**
     * Khởi tạo view binding
     */
    private void initBinding(Context context) {
        binding = MarkerViewBinding.inflate(LayoutInflater.from(context), this, true);
    }

    /**
     * Thiết lập danh sách tháng
     */
    public void setMonthLabels(List<String> monthLabels) {
        this.monthLabels = monthLabels;
    }

    @Override
    public void refreshContent(Entry e, Highlight highlight) {
        LineDataSet dataSet = (LineDataSet) ((LineChart)getChartView()).getData().getDataSetForEntry(e);
        int colorRes = dataSet.getLabel().equals("Chi tiêu") ?
                R.color.expense_color : R.color.income_color;

        int index = (int) e.getX();
        String month = index >= 0 && index < monthLabels.size() ? monthLabels.get(index) : "";

        String valueText;
        if (e.getY() > 1000000) {
            valueText = String.format(Locale.getDefault(), "%.1f triệu", e.getY() / 1000000);
        } else if (e.getY() > 1000) {
            valueText = String.format(Locale.getDefault(), "%.0f nghìn", e.getY() / 1000);
        } else {
            valueText = String.format(Locale.getDefault(), "%.0f đ", e.getY());
        }

        binding.tvContent.setText(String.format("%s\n%s: %s", month, dataSet.getLabel(), valueText));
        binding.tvContent.setTextColor(ContextCompat.getColor(getContext(), colorRes));

        super.refreshContent(e, highlight);
    }

    @Override
    public MPPointF getOffset() {
        return new MPPointF(-(getWidth() / 2f), -getHeight() - 10);
    }
}