package com.nhom08.qlychitieu.giao_dien.nguoi_dung;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.CustomCredential;
import androidx.credentials.exceptions.GetCredentialException;

import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.nhom08.qlychitieu.MyApplication;
import com.nhom08.qlychitieu.R;
import com.nhom08.qlychitieu.databinding.ActivityLogInBinding;
import com.nhom08.qlychitieu.csdl.AppDatabase;
import com.nhom08.qlychitieu.giao_dien.man_hinh_chinh.MainActivity;
import com.nhom08.qlychitieu.mo_hinh.User;
import com.nhom08.qlychitieu.tien_ich.Constants;
import com.nhom08.qlychitieu.tien_ich.MessageUtils;
import com.nhom08.qlychitieu.tien_ich.PasswordUtil;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class LogInActivity extends AppCompatActivity {
    private static final String TAG = "LogInActivity";

    private ActivityLogInBinding binding;
    private CredentialManager credentialManager;
    private MessageUtils messageUtils;
    private AppDatabase appDatabase;
    private final Executor executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean isPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: Started");

        // Kiểm tra trạng thái đăng nhập trước khi khởi tạo UI
        if (checkLoginState()) {
            navigateToMain();
            return;
        }

        Log.d(TAG, "onCreate: No saved login state, showing login screen");

        initializeUI();
        initializeDatabase();
        setupComponents();
        setupClickListeners();
    }
    private boolean checkLoginState() {
        SharedPreferences prefs = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
        boolean isLoggedIn = prefs.getBoolean(Constants.KEY_IS_LOGGED_IN, false);
        String savedEmail = prefs.getString(Constants.KEY_USER_EMAIL, null);

        return isLoggedIn && savedEmail != null;
    }
    private void initializeUI() {
        EdgeToEdge.enable(this);
        binding = ActivityLogInBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void saveLoginState(String email, String loginType) {
        SharedPreferences.Editor editor = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE).edit();
        editor.putBoolean(Constants.KEY_IS_LOGGED_IN, true)
                .putString(Constants.KEY_USER_EMAIL, email)
                .putString(Constants.KEY_LOGIN_TYPE, loginType)
                .apply();
    }


    private void initializeDatabase() {
        try {
            appDatabase = ((MyApplication) getApplication()).getDatabase();
            if (appDatabase == null) {
                Log.e(TAG, "initializeDatabase: AppDatabase is null");
                messageUtils.showError("Không thể khởi tạo cơ sở dữ liệu");
                finish();
            }
        } catch (Exception e) {
            Log.e(TAG, "initializeDatabase: Error", e);
            messageUtils.showError("Lỗi khởi tạo cơ sở dữ liệu");
            finish();
        }
    }

    private void setupComponents() {
        messageUtils = new MessageUtils(this);
        credentialManager = CredentialManager.create(this);
    }

    private void setupClickListeners() {
        binding.btnLogin.setOnClickListener(v -> loginUser());
        binding.btnGoogleLogin.setOnClickListener(v -> signInWithGoogle());
        binding.btnViewRegister.setOnClickListener(v -> navigateToSignUp());
        binding.iconPasswordVisibility.setOnClickListener(v -> togglePasswordVisibility());
        binding.btnForgotPassword.setOnClickListener(v -> handleForgotPassword());
    }

    private void loginUser() {
        String email = binding.edtUsername.getText().toString().trim();
        String password = binding.edtPassword.getText().toString();

        if (!validateLoginInputs(email, password)) {
            return;
        }

        executor.execute(() -> {
            try {
                User user = appDatabase.userDao().getUserByEmail(email);

                if (user == null) {
                    showError("Tài khoản không tồn tại");
                    return;
                }

                if (!PasswordUtil.checkPassword(password, user.getPassword())) {
                    showError("Sai mật khẩu");
                    return;
                }

                handleSuccessfulLogin(email);
            } catch (Exception e) {
                Log.e(TAG, "loginUser: Error", e);
                showError("Lỗi đăng nhập: " + e.getMessage());
            }
        });
    }

    private void handleForgotPassword() {
        // TODO: Implement forgot password functionality
        showError("Tính năng đang được phát triển");
    }

    private boolean validateLoginInputs(String email, String password) {
        if (TextUtils.isEmpty(email)) {
            messageUtils.showError("Vui lòng nhập email");
            binding.edtUsername.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(password)) {
            messageUtils.showError("Vui lòng nhập mật khẩu");
            binding.edtPassword.requestFocus();
            return false;
        }
        return true;
    }

    private void signInWithGoogle() {
        try {
            GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                    .setServerClientId(getString(R.string.default_web_client_id))
                    .setFilterByAuthorizedAccounts(false)
                    .setAutoSelectEnabled(false)
                    .build();

            GetCredentialRequest request = new GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build();

            credentialManager.getCredentialAsync(
                    this,
                    request,
                    null,
                    executor,
                    new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                        @Override
                        public void onResult(GetCredentialResponse result) {
                            handleGoogleSignInResult(result);
                        }

                        @Override
                        public void onError(@NonNull GetCredentialException e) {
                            Log.e(TAG, "Google sign in error", e);
                            showError("Đăng nhập Google thất bại: " + e.getLocalizedMessage());
                        }
                    }
            );
        } catch (Exception e) {
            Log.e(TAG, "signInWithGoogle: Error", e);
            showError("Lỗi đăng nhập Google");
        }
    }

    private void handleGoogleSignInResult(GetCredentialResponse response) {
        if (!(response.getCredential() instanceof CustomCredential) ||
                !GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL.equals(response.getCredential().getType())) {
            showError("Không nhận được thông tin Google hợp lệ");
            return;
        }

        try {
            GoogleIdTokenCredential googleCredential = GoogleIdTokenCredential.createFrom(response.getCredential().getData());
            String googleId = googleCredential.getId();

            executor.execute(() -> {
                try {
                    User user = appDatabase.userDao().getUserByGoogleId(googleId);

                    if (user == null) {
                        showError("Chưa có tài khoản, vui lòng đăng ký");
                        navigateToSignUp();
                        return;
                    }

                    handler.post(() -> {
                        saveLoginState(user.getEmail(), Constants.LOGIN_TYPE_GOOGLE);
                        showSuccess();
                        navigateToMain();
                    });
                } catch (Exception e) {
                    Log.e(TAG, "handleGoogleSignInResult: Error", e);
                    showError("Lỗi xác thực tài khoản Google");
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "handleGoogleSignInResult: Error parsing credential", e);
            showError("Lỗi xử lý thông tin Google");
        }
    }

    private void handleSuccessfulLogin(String email) {
        handler.post(() -> {
            saveLoginState(email, Constants.LOGIN_TYPE_NORMAL);
            showSuccess();
            navigateToMain();
        });
    }

    private void togglePasswordVisibility() {
        isPasswordVisible = !isPasswordVisible;
        int inputType = isPasswordVisible ?
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD :
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD;

        binding.edtPassword.setInputType(inputType);
        binding.iconPasswordVisibility.setText(getString(
                isPasswordVisible ? R.string.icon_visibility : R.string.icon_visibility_off
        ));
        binding.edtPassword.setSelection(binding.edtPassword.getText().length());
    }

    private void navigateToMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private void navigateToSignUp() {
        startActivity(new Intent(this, SignUpActivity.class));
        finish();
    }

    private void showError(String message) {
        handler.post(() -> messageUtils.showError(message));
    }

    private void showSuccess() {
        handler.post(() -> messageUtils.showSuccess("Đăng nhập thành công"));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}