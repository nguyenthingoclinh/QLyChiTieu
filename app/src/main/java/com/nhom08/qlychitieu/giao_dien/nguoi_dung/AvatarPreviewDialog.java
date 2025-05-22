package com.nhom08.qlychitieu.giao_dien.nguoi_dung;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.nhom08.qlychitieu.R;

public class AvatarPreviewDialog extends DialogFragment {

    private int imageResId;

    public AvatarPreviewDialog(int imageResId) {
        this.imageResId = imageResId;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_avatar_preview, null);
        ImageView imgPreview = view.findViewById(R.id.imgPreview);
        imgPreview.setImageResource(imageResId);

        ImageView btnClose = view.findViewById(R.id.btnClose);

        Dialog dialog = new Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.setContentView(view);

        // Bấm vào nút "X" để đóng dialog
        btnClose.setOnClickListener(v -> dismiss());

        // Đóng khi người dùng nhấn vào bất kỳ đâu
        //view.setOnClickListener(v -> dismiss());

        return dialog;
    }
}
