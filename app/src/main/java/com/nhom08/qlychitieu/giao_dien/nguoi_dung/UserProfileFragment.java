package com.nhom08.qlychitieu.giao_dien.nguoi_dung;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.nhom08.qlychitieu.MyApplication;
import com.nhom08.qlychitieu.R;
import com.nhom08.qlychitieu.databinding.FragmentUserProfileBinding;
import com.nhom08.qlychitieu.mo_hinh.User;

import java.util.concurrent.Executors;

public class UserProfileFragment extends Fragment {
    private FragmentUserProfileBinding binding;
    private MyApplication app;
    private User currentUser;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        app = MyApplication.getInstance();
        currentUser = app.getCurrentUser();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentUserProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (currentUser != null) {
            binding.name.setText(currentUser.getFullName());
            binding.email.setText(currentUser.getEmail());

            binding.imgAvatar.setOnClickListener(v -> openAvatarPreviewDialog());
            binding.btnEditName.setOnClickListener(v -> openEditNameDialog());
            binding.btnEditEmail.setOnClickListener(v -> openEditEmailDialog());
        }

        binding.btnBack.setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack()
        );
    }

    private void openAvatarPreviewDialog() {
        int avatarResId = R.drawable.ic_avatar_default; // hoặc lấy từ currentUser nếu ảnh từ internet
        AvatarPreviewDialog dialog = new AvatarPreviewDialog(avatarResId);
        dialog.show(getChildFragmentManager(), "AvatarPreviewDialog");
    }

    private void openEditNameDialog() {
        EditNameDialog dialog = new EditNameDialog(currentUser.getFullName(), updatedName -> {
            binding.name.setText(updatedName);
            currentUser.setFullName(updatedName);

            // Cập nhật DB
            Executors.newSingleThreadExecutor().execute(() ->
                    app.getDatabase().userDao().updateUserName(currentUser.getEmail(), updatedName)
            );

            app.setCurrentUser(currentUser);
        });
        dialog.show(getChildFragmentManager(), "EditNameDialog");
    }

    private void openEditEmailDialog() {
        EditEmailDialog dialog = new EditEmailDialog(currentUser.getEmail(), updatedEmail -> {
            String oldEmail = currentUser.getEmail();
            currentUser.setEmail(updatedEmail);
            binding.email.setText(updatedEmail);

            // Cập nhật DB
            Executors.newSingleThreadExecutor().execute(() ->
                    app.getDatabase().userDao().updateUserEmail(oldEmail, updatedEmail)
            );

            app.setCurrentUser(currentUser);

            // Cập nhật SharedPreferences
            SharedPreferences prefs = requireContext().getSharedPreferences("user_session", Context.MODE_PRIVATE);
            prefs.edit().putString("user_email", updatedEmail).apply();
        });
        dialog.show(getChildFragmentManager(), "EditEmailDialog");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
