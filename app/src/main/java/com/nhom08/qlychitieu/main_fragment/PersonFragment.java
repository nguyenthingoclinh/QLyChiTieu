package com.nhom08.qlychitieu.main_fragment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.nhom08.qlychitieu.giao_dien.danh_muc.CategoryActivity;
import com.nhom08.qlychitieu.MyApplication;
import com.nhom08.qlychitieu.R;
import com.nhom08.qlychitieu.csdl.AppDatabase;
import com.nhom08.qlychitieu.giao_dien.dang_nhap.LoginActivity;
import com.nhom08.qlychitieu.mo_hinh.User;
import com.nhom08.qlychitieu.truy_van.UserDAO;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PersonFragment extends Fragment {
    private static final String TAG = "PersonFragment";

    private AppDatabase database;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private SharedPreferences sharedPreferences;
    private boolean isFragmentAttached = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        isFragmentAttached = true;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_person, container, false);

        try {
            // Khởi tạo SharedPreferences
            if (getActivity() != null) {
                sharedPreferences = getActivity().getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE);
            }

            // Khởi tạo views (biến cục bộ)
            TextView tvUsername = view.findViewById(R.id.tvUsername);
            TextView tvUserEmail = view.findViewById(R.id.tvUserEmail);

            LinearLayout menuCategorySetting = view.findViewById(R.id.menuCategorySetting);
            LinearLayout menuAccount = view.findViewById(R.id.menuAccount);
            LinearLayout menuBudget = view.findViewById(R.id.menuBudget);
            LinearLayout menuExport = view.findViewById(R.id.menuExport);
            LinearLayout menuLogout = view.findViewById(R.id.menuLogout);

            // Xác nhận rằng các ID bạn đang truy cập khớp với XML
            Log.d(TAG, "menuCategorySetting: " + (menuCategorySetting != null ? "found" : "null"));
            Log.d(TAG, "menuAccount: " + (menuAccount != null ? "found" : "null"));

            // Lấy database từ MyApplication
            if (getActivity() != null) {
                database = ((MyApplication) getActivity().getApplication()).getDatabase();
            }

            // Thiết lập sự kiện click cho các mục menu
            setupMenuClickListeners(menuCategorySetting, menuAccount, menuBudget, menuExport, menuLogout);

            // Lấy dữ liệu từ Room Database
            if (isFragmentAttached) {
                fetchUserData(tvUsername, tvUserEmail);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error in onCreateView: " + e.getMessage(), e);
        }

        return view;
    }

    private void setupMenuClickListeners(LinearLayout menuCategorySetting, LinearLayout menuAccount,
                                         LinearLayout menuBudget, LinearLayout menuExport, LinearLayout menuLogout) {
        if (menuCategorySetting != null) {
            menuCategorySetting.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), CategoryActivity.class);
                startActivity(intent);
            });
        }

        if (menuAccount != null) {
            menuAccount.setOnClickListener(v -> {
                Toast.makeText(getContext(), "Mở Tài khoản", Toast.LENGTH_SHORT).show();
                // TODO: Thêm logic mở màn hình Tài khoản
            });
        }

        if (menuBudget != null) {
            menuBudget.setOnClickListener(v -> {
                Toast.makeText(getContext(), "Mở Ngân sách", Toast.LENGTH_SHORT).show();
                // TODO: Thêm logic mở màn hình Ngân sách
            });
        }

        if (menuExport != null) {
            menuExport.setOnClickListener(v -> {
                Toast.makeText(getContext(), "Mở Xuất dữ liệu", Toast.LENGTH_SHORT).show();
                // TODO: Thêm logic mở màn hình Xuất dữ liệu
            });
        }

        if (menuLogout != null) {
            menuLogout.setOnClickListener(v -> logout());
        }
    }

    private void fetchUserData(TextView tvUsername, TextView tvUserEmail) {
        if (!isFragmentAttached || getActivity() == null) return;

        executorService.execute(() -> {
            try {
                // Kiểm tra SharedPreferences
                if (sharedPreferences == null) return;

                // Lấy email của người dùng đã đăng nhập
                String loggedInEmail = sharedPreferences.getString("loggedInEmail", null);
                Log.d(TAG, "loggedInEmail: " + loggedInEmail);
                if (loggedInEmail == null) {
                    navigateToLogin();
                    return;
                }

                // Kiểm tra database
                if (database == null) return;

                // Lấy thông tin người dùng
                UserDAO userDao = database.userDao();
                User user = userDao.getUserByEmail(loggedInEmail);

                if (!isFragmentAttached) return;

                // Cập nhật UI
                getActivity().runOnUiThread(() -> {
                    if (isFragmentAttached && user != null) {
                        updateUserInterface(user, tvUsername, tvUserEmail);
                    } else {
                        logout();
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error in fetchUserData: " + e.getMessage(), e);
                if (isFragmentAttached) {
                    getActivity().runOnUiThread(this::logout);
                }
            }
        });
    }

    private void updateUserInterface(User user, TextView tvUsername, TextView tvUserEmail) {
        if (!isFragmentAttached) return;

        try {
            if (tvUsername != null) {
                tvUsername.setText(user.getFullName());
            }
            if (tvUserEmail != null) {
                tvUserEmail.setText(user.getEmail());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in updateUserInterface: " + e.getMessage(), e);
        }
    }

    private void navigateToLogin() {
        if (!isFragmentAttached || getActivity() == null) return;

        getActivity().runOnUiThread(() -> {
            try {
                Intent intent = new Intent(getActivity(), LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                getActivity().finish();
            } catch (Exception e) {
                Log.e(TAG, "Error in navigateToLogin: " + e.getMessage(), e);
            }
        });
    }

    private void logout() {
        try {
            if (sharedPreferences != null) {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.remove("loggedInEmail");
                editor.apply();
            }

            navigateToLogin();
        } catch (Exception e) {
            Log.e(TAG, "Error in logout: " + e.getMessage(), e);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        isFragmentAttached = false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (!executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}