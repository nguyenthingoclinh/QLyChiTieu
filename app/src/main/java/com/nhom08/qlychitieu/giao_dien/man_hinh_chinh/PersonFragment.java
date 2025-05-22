package com.nhom08.qlychitieu.giao_dien.man_hinh_chinh;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.nhom08.qlychitieu.MyApplication;
import com.nhom08.qlychitieu.R;
import com.nhom08.qlychitieu.databinding.FragmentPersonBinding;
import com.nhom08.qlychitieu.giao_dien.SettingActivity;
import com.nhom08.qlychitieu.giao_dien.danh_muc.CategoryActivity;
import com.nhom08.qlychitieu.giao_dien.nguoi_dung.LogInActivity;
import com.nhom08.qlychitieu.giao_dien.thong_bao.NotificationSettingsActivity;
import com.nhom08.qlychitieu.giao_dien.ExportImportActivity;
import com.nhom08.qlychitieu.mo_hinh.User;
import com.nhom08.qlychitieu.tien_ich.MessageUtils;

/**
 * Fragment quản lý thông tin cá nhân và cài đặt
 */
public class PersonFragment extends Fragment {
    private FragmentPersonBinding binding;
    private MessageUtils messageUtils;
    private MyApplication app;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        app = MyApplication.getInstance();
        messageUtils = new MessageUtils(requireContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentPersonBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupViews();
        updateUserInfo();
    }

    private void setupViews() {
        binding.menuCategorySetting.setOnClickListener(v -> openCategorySettings());
        binding.menuSetting.setOnClickListener(v -> openSettings());
        binding.menuAlert.setOnClickListener(v -> openAlert());
        binding.menuExport.setOnClickListener(v -> openExportImport());
        binding.menuLogout.setOnClickListener(v -> handleLogout());
    }

    private void updateUserInfo() {
        User currentUser = app.getCurrentUser();
        if (currentUser != null) {
            binding.tvUsername.setText(currentUser.getFullName());
            binding.tvUserEmail.setText(currentUser.getEmail());
        } else {
            navigateToLogin();
        }
    }

    private void openCategorySettings() {
        startActivity(new Intent(requireContext(), CategoryActivity.class));
    }

    private void openSettings() {
        startActivity(new Intent(requireContext(), SettingActivity.class));
    }
    private void openAlert() {
        startActivity(new Intent(requireContext(), NotificationSettingsActivity.class));
    }

    private void openExportImport() {
        startActivity(new Intent(requireContext(), ExportImportActivity.class));
    }

    private void handleLogout() {
        try {
            app.clearUserSession();
            navigateToLogin();
            messageUtils.showSuccess(R.string.success_logout);
        } catch (Exception e) {
            messageUtils.showError(R.string.error_logout);
        }
    }

    private void navigateToLogin() {
        Intent intent = new Intent(requireContext(), LogInActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}