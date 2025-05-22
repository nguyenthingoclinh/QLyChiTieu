package com.nhom08.qlychitieu.giao_dien.nguoi_dung;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.nhom08.qlychitieu.R;

public class EditEmailDialog extends DialogFragment {
    private String currentEmail;
    private OnEmailUpdatedListener listener;

    public interface OnEmailUpdatedListener {
        void onEmailUpdated(String updatedEmail);
    }

    public EditEmailDialog(String currentEmail, OnEmailUpdatedListener listener) {
        this.currentEmail = currentEmail;
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        // Inflate layout
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_email, null);
        EditText edtEmail = view.findViewById(R.id.edtNewEmail);
        Button btnSave = view.findViewById(R.id.btnSave);
        Button btnCancel = view.findViewById(R.id.btnCancel);

        edtEmail.setText(currentEmail);

        // Xử lý nút Hủy
        btnCancel.setOnClickListener(v -> dismiss());

        // Xử lý nút Lưu
        btnSave.setOnClickListener(v -> {
            String updatedEmail = edtEmail.getText().toString().trim();

            if (updatedEmail.isEmpty()) {
                edtEmail.setError("Email không được để trống");
            } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(updatedEmail).matches()) {
                edtEmail.setError("Email không hợp lệ");
            } else {
                listener.onEmailUpdated(updatedEmail);
                dismiss(); // Đóng dialogFragment một cách chính thống
            }
        });

        // Trả về dialog
        return new MaterialAlertDialogBuilder(requireContext())
                .setView(view)
                .create();
    }
}
