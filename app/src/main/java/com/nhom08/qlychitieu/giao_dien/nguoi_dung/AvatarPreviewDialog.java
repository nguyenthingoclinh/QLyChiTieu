package com.nhom08.qlychitieu.giao_dien.nguoi_dung;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.bumptech.glide.Glide;
import com.nhom08.qlychitieu.R;

import java.io.File;

public class AvatarPreviewDialog extends DialogFragment {

    private String imagePath;

    public AvatarPreviewDialog(String imagePath) {
        this.imagePath = imagePath;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_avatar_preview, null);
        ImageView imgPreview = view.findViewById(R.id.imgPreview);
        ImageView btnClose = view.findViewById(R.id.btnClose);

        if (imagePath != null && !imagePath.isEmpty()) {
            Glide.with(requireContext())
                    .load(new File(imagePath))
                    .placeholder(R.drawable.ic_avatar_default)
                    .into(imgPreview);
        } else {
            imgPreview.setImageResource(R.drawable.ic_avatar_default);
        }

        Dialog dialog = new Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.setContentView(view);

        btnClose.setOnClickListener(v -> dismiss());

        return dialog;
    }
}
