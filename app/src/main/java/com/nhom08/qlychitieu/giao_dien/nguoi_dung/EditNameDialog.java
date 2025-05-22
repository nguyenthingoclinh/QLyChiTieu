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

public class EditNameDialog extends DialogFragment {
    private final String currentName;
    private final OnNameUpdatedListener listener;

    public interface OnNameUpdatedListener {
        void onNameUpdated(String updatedName);
    }

    public EditNameDialog(String currentName, OnNameUpdatedListener listener) {
        this.currentName = currentName;
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_name, null);
        EditText edtName = view.findViewById(R.id.edtNewName);
        Button btnSave = view.findViewById(R.id.btnSave);
        Button btnCancel = view.findViewById(R.id.btnCancel);

        edtName.setText(currentName);

        // Xử lý nút Hủy
        btnCancel.setOnClickListener(v -> dismiss());

        // Xử lý nút Lưu
        btnSave.setOnClickListener(v -> {
            String updatedName = edtName.getText().toString().trim();
            if (updatedName.isEmpty()) {
                edtName.setError("Tên không được để trống");
            } else {
                listener.onNameUpdated(updatedName);
                dismiss();
            }
        });

        return new MaterialAlertDialogBuilder(requireContext())
                .setView(view)
                .create();
    }
}
