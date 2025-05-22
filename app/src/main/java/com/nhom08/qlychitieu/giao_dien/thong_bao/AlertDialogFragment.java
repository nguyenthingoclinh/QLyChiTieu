package com.nhom08.qlychitieu.giao_dien.thong_bao;

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.nhom08.qlychitieu.databinding.DialogAlertBinding;
import com.nhom08.qlychitieu.mo_hinh.SpendingAlert;
import com.nhom08.qlychitieu.tien_ich.Constants;

/**
 * Dialog Fragment để thêm hoặc sửa cảnh báo chi tiêu
 */
public class AlertDialogFragment extends DialogFragment {

    private DialogAlertBinding binding;
    private SpendingAlert currentAlert;
    private OnAlertSaveListener listener;
    private int currentUserId;

    /**
     * Interface để lắng nghe sự kiện lưu cảnh báo
     */
    public interface OnAlertSaveListener {
        void onAlertSaved(SpendingAlert alert);
    }

    /**
     * Tạo instance mới của dialog để thêm cảnh báo mới
     */
    public static AlertDialogFragment newInstance(int userId) {
        AlertDialogFragment fragment = new AlertDialogFragment();
        Bundle args = new Bundle();
        args.putInt("userId", userId);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Tạo instance mới của dialog để sửa cảnh báo hiện có
     */
    public static AlertDialogFragment newInstance(SpendingAlert alert) {
        AlertDialogFragment fragment = new AlertDialogFragment();
        Bundle args = new Bundle();
        args.putInt("alertId", alert.getAlertId());
        args.putInt("userId", alert.getUserId());
        args.putString("title", alert.getTitle());
        args.putString("alertType", alert.getAlertType());
        args.putDouble("threshold", alert.getThreshold());
        args.putBoolean("active", alert.isActive());
        args.putString("notifyTime", alert.getNotifyTime());
        args.putInt("priority", alert.getPriority());
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Đặt listener cho sự kiện lưu cảnh báo
     */
    public void setOnAlertSaveListener(OnAlertSaveListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DialogAlertBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Lấy userId từ arguments
        currentUserId = getArguments().getInt("userId", -1);

        // Khởi tạo dữ liệu
        initializeData();

        // Thiết lập listener
        setupListeners();
    }

    /**
     * Khởi tạo dữ liệu từ arguments
     */
    private void initializeData() {
        Bundle args = getArguments();
        boolean isEditing = false;
        if (args != null && args.containsKey("alertId")) {
            // Đang trong chế độ sửa
            isEditing = true;

            // Tạo đối tượng cảnh báo hiện tại từ arguments
            currentAlert = new SpendingAlert();
            currentAlert.setAlertId(args.getInt("alertId"));
            currentAlert.setUserId(args.getInt("userId"));
            currentAlert.setTitle(args.getString("title"));
            currentAlert.setAlertType(args.getString("alertType"));
            currentAlert.setThreshold(args.getDouble("threshold"));
            currentAlert.setActive(args.getBoolean("active"));
            currentAlert.setNotifyTime(args.getString("notifyTime"));
            currentAlert.setPriority(args.getInt("priority"));

            // Điền dữ liệu vào UI
            binding.etAlertTitle.setText(currentAlert.getTitle());
            binding.etThreshold.setText(String.valueOf((int)currentAlert.getThreshold()));
            binding.tvAlertTime.setText(currentAlert.getNotifyTime());

            // Chọn loại cảnh báo
            switch (currentAlert.getAlertType()) {
                case Constants.ALERT_TYPE_DAILY:
                    binding.rbDaily.setChecked(true);
                    break;
                case Constants.ALERT_TYPE_WEEKLY:
                    binding.rbWeekly.setChecked(true);
                    break;
                case Constants.ALERT_TYPE_MONTHLY:
                    binding.rbMonthly.setChecked(true);
                    break;
            }

            // Chọn mức độ ưu tiên
            switch (currentAlert.getPriority()) {
                case Constants.ALERT_PRIORITY_LOW:
                    binding.rbLow.setChecked(true);
                    break;
                case Constants.ALERT_PRIORITY_MEDIUM:
                    binding.rbMedium.setChecked(true);
                    break;
                case Constants.ALERT_PRIORITY_HIGH:
                    binding.rbHigh.setChecked(true);
                    break;
            }
        } else {
            // Đang trong chế độ thêm mới
            isEditing = false;

            // Khởi tạo đối tượng cảnh báo mới
            currentAlert = new SpendingAlert(
                    currentUserId,
                    "",
                    Constants.ALERT_TYPE_DAILY,
                    0.0
            );

            // Mặc định là loại hàng ngày và mức độ trung bình (đã được chọn trong layout)
        }
    }

    /**
     * Thiết lập các listener cho UI
     */
    private void setupListeners() {
        // Bộ chọn thời gian
        binding.tvAlertTime.setOnClickListener(v -> {
            String[] timeParts = binding.tvAlertTime.getText().toString().split(":");
            int hour = Integer.parseInt(timeParts[0]);
            int minute = Integer.parseInt(timeParts[1]);

            TimePickerDialog timePickerDialog = new TimePickerDialog(
                    getContext(),
                    (view, hourOfDay, minute1) -> {
                        String newTime = String.format("%02d:%02d", hourOfDay, minute1);
                        binding.tvAlertTime.setText(newTime);
                    },
                    hour,
                    minute,
                    true  // 24h format
            );
            timePickerDialog.show();
        });

        // Nút Hủy
        binding.btnCancel.setOnClickListener(v -> dismiss());

        // Nút Lưu
        binding.btnSave.setOnClickListener(v -> {
            if (validateInput()) {
                saveAlert();
            }
        });

        // Format số tiền khi nhập
        binding.etThreshold.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                // Xử lý định dạng số tiền nếu cần
            }
        });
    }

    /**
     * Kiểm tra và xác thực đầu vào
     */
    private boolean validateInput() {
        boolean isValid = true;

        // Kiểm tra tiêu đề
        String title = binding.etAlertTitle.getText().toString().trim();
        if (TextUtils.isEmpty(title)) {
            binding.tilAlertTitle.setError("Vui lòng nhập tiêu đề cảnh báo");
            isValid = false;
        } else {
            binding.tilAlertTitle.setError(null);
        }

        // Kiểm tra ngưỡng chi tiêu
        String thresholdStr = binding.etThreshold.getText().toString().trim();
        if (TextUtils.isEmpty(thresholdStr)) {
            binding.tilThreshold.setError("Vui lòng nhập ngưỡng chi tiêu");
            isValid = false;
        } else {
            try {
                double threshold = Double.parseDouble(thresholdStr);
                if (threshold <= 0) {
                    binding.tilThreshold.setError("Ngưỡng chi tiêu phải lớn hơn 0");
                    isValid = false;
                } else {
                    binding.tilThreshold.setError(null);
                }
            } catch (NumberFormatException e) {
                binding.tilThreshold.setError("Giá trị không hợp lệ");
                isValid = false;
            }
        }

        return isValid;
    }

    /**
     * Lưu cảnh báo
     */
    private void saveAlert() {
        // Lấy tiêu đề
        String title = binding.etAlertTitle.getText().toString().trim();

        // Lấy loại cảnh báo
        String alertType;
        if (binding.rbDaily.isChecked()) {
            alertType = Constants.ALERT_TYPE_DAILY;
        } else if (binding.rbWeekly.isChecked()) {
            alertType = Constants.ALERT_TYPE_WEEKLY;
        } else {
            alertType = Constants.ALERT_TYPE_MONTHLY;
        }

        // Lấy ngưỡng chi tiêu
        double threshold = Double.parseDouble(binding.etThreshold.getText().toString());

        // Lấy thời gian thông báo
        String notifyTime = binding.tvAlertTime.getText().toString();

        // Lấy mức độ ưu tiên
        int priority;
        if (binding.rbLow.isChecked()) {
            priority = Constants.ALERT_PRIORITY_LOW;
        } else if (binding.rbMedium.isChecked()) {
            priority = Constants.ALERT_PRIORITY_MEDIUM;
        } else {
            priority = Constants.ALERT_PRIORITY_HIGH;
        }

        // Cập nhật đối tượng cảnh báo
        currentAlert.setTitle(title);
        currentAlert.setAlertType(alertType);
        currentAlert.setThreshold(threshold);
        currentAlert.setNotifyTime(notifyTime);
        currentAlert.setPriority(priority);
        currentAlert.setActive(true); // Mặc định là kích hoạt khi thêm/sửa

        // Gọi callback
        if (listener != null) {
            listener.onAlertSaved(currentAlert);
        }

        // Đóng dialog
        dismiss();
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            // Set dialog width to match parent
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}