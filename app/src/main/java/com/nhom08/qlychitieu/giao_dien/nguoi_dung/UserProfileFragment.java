package com.nhom08.qlychitieu.giao_dien.nguoi_dung;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.nhom08.qlychitieu.MyApplication;
import com.nhom08.qlychitieu.R;
import com.nhom08.qlychitieu.databinding.FragmentUserProfileBinding;
import com.nhom08.qlychitieu.mo_hinh.User;
import com.nhom08.qlychitieu.tien_ich.SharedPrefUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.Executors;

public class UserProfileFragment extends Fragment {
    private FragmentUserProfileBinding binding;
    private MyApplication app;
    private User currentUser;
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private static final int REQUEST_READ_EXTERNAL_STORAGE = 1001;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        app = MyApplication.getInstance();
        currentUser = app.getCurrentUser();

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri selectedImageUri = result.getData().getData();
                        if (selectedImageUri != null) {
                            String savedPath = saveToAvatarFolder(selectedImageUri);
                            if (savedPath != null) {
                                currentUser.setAvatarPath(savedPath);
                                Executors.newSingleThreadExecutor().execute(() -> {
                                    app.getUserDAO().updateUser(currentUser);
                                });
                                SharedPrefUtils.saveUser(requireContext(), currentUser);

                                Glide.with(this)
                                        .load(new File(savedPath))
                                        .placeholder(R.drawable.ic_avatar_default)
                                        .into(binding.imgAvatar);
                            }
                        }
                    }
                });
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

            if (currentUser.getAvatarPath() != null && !currentUser.getAvatarPath().isEmpty()) {
                Glide.with(this)
                        .load(new File(currentUser.getAvatarPath()))
                        .placeholder(R.drawable.ic_avatar_default)
                        .into(binding.imgAvatar);
            }

            binding.imgAvatar.setOnClickListener(v -> openAvatarPreviewDialog());

            binding.btnEditAvatar.setOnClickListener(v -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.READ_MEDIA_IMAGES)
                            != PackageManager.PERMISSION_GRANTED) {
                        requestPermissions(new String[]{android.Manifest.permission.READ_MEDIA_IMAGES}, REQUEST_READ_EXTERNAL_STORAGE);
                    } else {
                        openGallery();
                    }
                } else {
                    if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.READ_EXTERNAL_STORAGE)
                            != PackageManager.PERMISSION_GRANTED) {
                        requestPermissions(new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_READ_EXTERNAL_STORAGE);
                    } else {
                        openGallery();
                    }
                }
            });

            binding.btnEditName.setOnClickListener(v -> openEditNameDialog());
            binding.btnEditEmail.setOnClickListener(v -> openEditEmailDialog());
        }

        binding.btnBack.setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack()
        );
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        if (intent.resolveActivity(requireContext().getPackageManager()) != null) {
            imagePickerLauncher.launch(intent);
        } else {
            Toast.makeText(requireContext(), "Không tìm thấy ứng dụng chọn ảnh", Toast.LENGTH_SHORT).show();
        }
    }

    private String saveToAvatarFolder(Uri imageUri) {
        try {
            File folder = new File(requireContext().getFilesDir(), "AvatarUser");
            if (!folder.exists()) {
                folder.mkdirs();
            }

            String fileName = "avatar_" + System.currentTimeMillis() + ".jpg";
            File imageFile = new File(folder, fileName);

            InputStream inputStream = requireContext().getContentResolver().openInputStream(imageUri);
            OutputStream outputStream = new FileOutputStream(imageFile);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }

            outputStream.close();
            inputStream.close();

            return imageFile.getAbsolutePath();

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void openAvatarPreviewDialog() {
        String avatarPath = currentUser.getAvatarPath();
        AvatarPreviewDialog dialog = new AvatarPreviewDialog(avatarPath);
        dialog.show(getChildFragmentManager(), "AvatarPreviewDialog");
    }

    private void openEditNameDialog() {
        EditNameDialog dialog = new EditNameDialog(currentUser.getFullName(), updatedName -> {
            binding.name.setText(updatedName);
            currentUser.setFullName(updatedName);

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

            Executors.newSingleThreadExecutor().execute(() ->
                    app.getDatabase().userDao().updateUserEmail(oldEmail, updatedEmail)
            );

            app.setCurrentUser(currentUser);

            SharedPreferences prefs = requireContext().getSharedPreferences("user_session", Context.MODE_PRIVATE);
            prefs.edit().putString("user_email", updatedEmail).apply();
        });
        dialog.show(getChildFragmentManager(), "EditEmailDialog");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_READ_EXTERNAL_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openGallery();
            } else {
                Toast.makeText(requireContext(), "Bạn cần cấp quyền để chọn ảnh", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
