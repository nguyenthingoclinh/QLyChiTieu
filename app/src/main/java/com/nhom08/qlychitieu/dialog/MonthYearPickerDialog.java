package com.nhom08.qlychitieu.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import com.nhom08.qlychitieu.R;
import com.nhom08.qlychitieu.databinding.DialogMonthYearPickerBinding;
import java.util.Calendar;

public class MonthYearPickerDialog extends DialogFragment {
    private static final String[] MONTHS = {
            "Tháng 1", "Tháng 2", "Tháng 3", "Tháng 4", "Tháng 5", "Tháng 6",
            "Tháng 7", "Tháng 8", "Tháng 9", "Tháng 10", "Tháng 11", "Tháng 12"
    };

    public interface OnDateSetListener {
        void onDateSet(int year, int month);
    }

    private final Calendar calendar;
    private final OnDateSetListener listener;

    public MonthYearPickerDialog(Calendar calendar, OnDateSetListener listener) {
        this.calendar = calendar;
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        DialogMonthYearPickerBinding binding = DialogMonthYearPickerBinding.inflate(
                getLayoutInflater());

        setupPickers(binding);

        return new AlertDialog.Builder(requireActivity())
                .setView(binding.getRoot())
                .setPositiveButton("OK", (dialog, id) -> {
                    listener.onDateSet(
                            binding.yearPicker.getValue(),
                            binding.monthPicker.getValue()
                    );
                })
                .setNegativeButton("Hủy", null)
                .create();
    }

    private void setupPickers(DialogMonthYearPickerBinding binding) {
        // Setup month picker
        binding.monthPicker.setMinValue(0);
        binding.monthPicker.setMaxValue(11);
        binding.monthPicker.setDisplayedValues(MONTHS);
        binding.monthPicker.setValue(calendar.get(Calendar.MONTH));

        // Setup year picker
        Calendar now = Calendar.getInstance();
        binding.yearPicker.setMinValue(2020);
        binding.yearPicker.setMaxValue(now.get(Calendar.YEAR));
        binding.yearPicker.setValue(calendar.get(Calendar.YEAR));
    }
}