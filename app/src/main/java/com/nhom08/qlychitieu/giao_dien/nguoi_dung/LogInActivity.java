package com.nhom08.qlychitieu.giao_dien.nguoi_dung;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
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

public class LogInActivity extends AppCompatActivity {
    private static final String TAG = "LogInActivity";

    private ActivityLogInBinding binding;
    private CredentialManager credentialManager;
    private MessageUtils messageUtils;
    private AppDatabase appDatabase;
    private MyApplication app;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: Started");

        // Kiểm tra trạng thái đăng nhập trước
        if (checkLoginState()) {
            navigateToMain();
            return;
        }

        initializeComponents();
        setupViews();
    }

    private void initializeComponents() {
        app = MyApplication.getInstance();
        messageUtils = new MessageUtils(this);
        appDatabase = app.getDatabase();
        credentialManager = CredentialManager.create(this);

        if (appDatabase == null) {
            messageUtils.showError(R.string.error_db_init);
            finish();
        }
    }

    private void setupViews() {
        binding = ActivityLogInBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnLogin.setOnClickListener(v -> loginUser());
        binding.btnGoogleLogin.setOnClickListener(v -> signInWithGoogle());
        binding.btnViewRegister.setOnClickListener(v -> navigateToSignUp());
        binding.btnForgotPassword.setOnClickListener(v -> handleForgotPassword());
    }

    private boolean checkLoginState() {
        SharedPreferences prefs = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
        return prefs.getBoolean(Constants.KEY_IS_LOGGED_IN, false) &&
                prefs.getString(Constants.KEY_USER_EMAIL, null) != null;
    }

    private void loginUser() {
        String email = String.valueOf(binding.edtEmail.getText()).trim();
        String password = String.valueOf(binding.edtPassword.getText());

        if (!validateLoginInputs(email, password)) {
            return;
        }

        app.getExecutorService().execute(() -> {
            try {
                User user = appDatabase.userDao().getUserByEmail(email);

                if (user == null) {
                    showError(getString(R.string.error_account_not_exist));
                    return;
                }

                if (!PasswordUtil.checkPassword(password, user.getPassword())) {
                    showError(getString(R.string.error_wrong_password));
                    return;
                }

                handleSuccessfulLogin(user);
            } catch (Exception e) {
                Log.e(TAG, "loginUser: Error", e);
                showError(getString(R.string.error_login_failed));
            }
        });
    }

    private boolean validateLoginInputs(String email, String password) {
        if (TextUtils.isEmpty(email)) {
            binding.edtEmail.setError(getString(R.string.error_empty_email));
            binding.edtEmail.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(password)) {
            binding.edtPassword.setError(getString(R.string.error_empty_password));
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
                    app.getExecutorService(),
                    new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                        @Override
                        public void onResult(GetCredentialResponse result) {
                            handleGoogleSignInResult(result);
                        }

                        @Override
                        public void onError(@NonNull GetCredentialException e) {
                            showError(getString(R.string.error_google_signin));
                        }
                    }
            );
        } catch (Exception e) {
            Log.e(TAG, "signInWithGoogle: Error", e);
            showError(getString(R.string.error_google_signin));
        }
    }

    private void handleGoogleSignInResult(GetCredentialResponse response) {
        if (!(response.getCredential() instanceof CustomCredential) ||
                !GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL.equals(
                        response.getCredential().getType())) {
            showError(getString(R.string.error_invalid_google_credential));
            return;
        }

        try {
            GoogleIdTokenCredential googleCredential = GoogleIdTokenCredential.createFrom(
                    response.getCredential().getData());
            String googleId = googleCredential.getId();

            app.getExecutorService().execute(() -> {
                try {
                    User user = appDatabase.userDao().getUserByGoogleId(googleId);
                    if (user == null) {
                        showError(getString(R.string.error_need_register));
                        navigateToSignUp();
                        return;
                    }
                    handleSuccessfulLogin(user);
                } catch (Exception e) {
                    Log.e(TAG, "handleGoogleSignInResult: Error", e);
                    showError(getString(R.string.error_google_auth));
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "handleGoogleSignInResult: Error parsing credential", e);
            showError(getString(R.string.error_google_info));
        }
    }

    private void handleSuccessfulLogin(User user) {
        handler.post(() -> {
            saveLoginState(user);
            messageUtils.showSuccess(R.string.login_success);
            navigateToMain();
        });
    }

    private void saveLoginState(User user) {
        app.setCurrentUser(user);
        SharedPreferences.Editor editor = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE).edit();
        editor.putBoolean(Constants.KEY_IS_LOGGED_IN, true)
                .putString(Constants.KEY_USER_EMAIL, user.getEmail())
                .putString(Constants.KEY_LOGIN_TYPE, user.getGoogleId() != null ?
                        Constants.LOGIN_TYPE_GOOGLE : Constants.LOGIN_TYPE_NORMAL)
                .apply();
    }

    private void handleForgotPassword() {
        // TODO: Implement forgot password functionality
        messageUtils.showError(R.string.feature_in_development);
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}