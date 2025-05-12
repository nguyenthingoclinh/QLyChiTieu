package com.nhom08.qlychitieu.giao_dien.nguoi_dung;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.CustomCredential;
import androidx.credentials.exceptions.GetCredentialException;

import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.nhom08.qlychitieu.MyApplication;
import com.nhom08.qlychitieu.R;
import com.nhom08.qlychitieu.databinding.ActivitySignUpBinding;
import com.nhom08.qlychitieu.csdl.AppDatabase;
import com.nhom08.qlychitieu.mo_hinh.Category;
import com.nhom08.qlychitieu.mo_hinh.User;
import com.nhom08.qlychitieu.tien_ich.MessageUtils;
import com.nhom08.qlychitieu.tien_ich.PasswordUtil;
import com.nhom08.qlychitieu.tien_ich.Constants;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class SignUpActivity extends AppCompatActivity {
    private static final String TAG = "SignUpActivity";
    private ActivitySignUpBinding binding;
    private CredentialManager credentialManager;
    private AppDatabase appDatabase;
    private MyApplication app;
    private MessageUtils messageUtils;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initializeUI();
        initializeComponents();
        setupCredentialManager();
        setupClickListeners();
        setupPasswordField();
    }

    private void initializeUI() {
        binding = ActivitySignUpBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
    }

    private void initializeComponents() {
        messageUtils = new MessageUtils(this);
        app = MyApplication.getInstance();
        appDatabase = app.getDatabase();
        if (appDatabase == null) {
            handleError("initializeDatabase", getString(R.string.error_db_init), null);
        }
    }

    private void setupCredentialManager() {
        Log.d(TAG, "setupCredentialManager: Initializing");
        credentialManager = CredentialManager.create(this);
    }

    private void setupClickListeners() {
        binding.btnRegister.setOnClickListener(v -> registerUser());
        binding.btnGoogleSignUp.setOnClickListener(v -> signInWithGoogle());
        binding.tvLoginRedirect.setOnClickListener(v -> navigateToLogin());
    }

    private void registerUser() {
        String fullName = String.valueOf(binding.edtFullName.getText()).trim();
        String email = String.valueOf(binding.edtEmail.getText()).trim();
        String password = String.valueOf(binding.edtPassword.getText());
        String confirmPassword = String.valueOf(binding.edtConfirmPassword.getText());

        if (!validateInputs(fullName, email, password, confirmPassword)) {
            return;
        }

        showLoading(true);
        app.getExecutorService().execute(() -> {
            try {
                if (isEmailExists(email)) {
                    handler.post(() -> {
                        messageUtils.showError(R.string.error_email_exists);
                        showLoading(false);
                    });
                    return;
                }

                User newUser = createUser(fullName, email, password);
                long userId = appDatabase.userDao().insertUser(newUser);
                addDefaultCategories((int) userId);

                handler.post(() -> {
                    showLoading(false);
                    messageUtils.showSuccess(R.string.signup_success);
                    navigateToLogin();
                });

            } catch (Exception e) {
                handleError("registerUser", getString(R.string.error_registration_failed), e);
                handler.post(() -> showLoading(false));
            }
        });
    }

    private boolean validateInputs(String fullName, String email, String password, String confirmPassword) {
        boolean isValid = true;

        if (TextUtils.isEmpty(fullName)) {
            binding.edtFullName.setError(getString(R.string.error_empty_fullname));
            isValid = false;
        }

        if (!Pattern.matches(Constants.EMAIL_PATTERN, email)) {
            binding.edtEmail.setError(getString(R.string.error_invalid_email));
            isValid = false;
        }

        if (!Pattern.matches(Constants.PASSWORD_PATTERN, password)) {
            binding.layoutPassword.setError(getString(R.string.error_invalid_password_format));
            isValid = false;
        } else {
            binding.layoutPassword.setError(null);
        }

        if (!password.equals(confirmPassword)) {
            binding.layoutConfirmPassword.setError(getString(R.string.error_password_mismatch));
            isValid = false;
        } else {
            binding.layoutConfirmPassword.setError(null);
        }

        return isValid;
    }

    private boolean isEmailExists(String email) {
        return appDatabase.userDao().getUserByEmail(email) != null;
    }

    private User createUser(String fullName, String email, String password) {
        return new User(
                fullName,
                email,
                PasswordUtil.hashPassword(password),
                null, // googleId
                null, // resetCode
                null  // avatarPath
        );
    }

    private void signInWithGoogle() {
        try {
            GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                    .setServerClientId(getString(R.string.default_web_client_id))
                    .setFilterByAuthorizedAccounts(false)
                    .setAutoSelectEnabled(true)
                    .build();

            GetCredentialRequest request = new GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build();

            credentialManager.getCredentialAsync(
                    this,
                    request,
                    null,
                    app.getExecutorService(),
                    new CredentialManagerCallback<>() {
                        @Override
                        public void onResult(GetCredentialResponse result) {
                            handleGoogleSignInResult(result);
                        }

                        @Override
                        public void onError(@NonNull GetCredentialException e) {
                            handleError("Google sign in", getString(R.string.error_google_signin), e);
                        }
                    }
            );
        } catch (Exception e) {
            handleError("signInWithGoogle", getString(R.string.error_google_signin), e);
        }
    }

    private void handleGoogleSignInResult(GetCredentialResponse response) {
        try {
            if (!(response.getCredential() instanceof CustomCredential) ||
                    !GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL.equals(
                            response.getCredential().getType())) {
                messageUtils.showError(R.string.error_invalid_google_credential);
                return;
            }

            GoogleIdTokenCredential googleCredential = GoogleIdTokenCredential.createFrom(
                    response.getCredential().getData());
            AuthCredential firebaseCredential = GoogleAuthProvider.getCredential(
                    googleCredential.getIdToken(), null);

            FirebaseAuth.getInstance().signInWithCredential(firebaseCredential)
                    .addOnSuccessListener(authResult -> {
                        FirebaseUser firebaseUser = authResult.getUser();
                        if (firebaseUser == null) {
                            messageUtils.showError(R.string.error_user_info);
                            return;
                        }
                        handleFirebaseUser(firebaseUser, googleCredential);
                    })
                    .addOnFailureListener(e ->
                            handleError("Firebase auth", getString(R.string.error_auth_failed), e));

        } catch (Exception e) {
            handleError("handleGoogleSignInResult", getString(R.string.error_google_signin_process), e);
        }
    }

    private void handleFirebaseUser(FirebaseUser firebaseUser, GoogleIdTokenCredential googleCredential) {
        String googleId = googleCredential.getId();
        String email = firebaseUser.getEmail();
        String fullName = firebaseUser.getDisplayName();
        String photoUrl = firebaseUser.getPhotoUrl() != null ? firebaseUser.getPhotoUrl().toString() : null;

        showLoading(true);
        app.getExecutorService().execute(() -> {
            try {
                User existingUser = appDatabase.userDao().getUserByGoogleId(googleId);
                if (existingUser != null) {
                    handler.post(() -> {
                        showLoading(false);
                        messageUtils.showError(R.string.error_google_account_exists);
                        navigateToLogin();
                    });
                    return;
                }

                User newUser = new User(fullName, email, null, googleId, null, photoUrl);
                long userId = appDatabase.userDao().insertUser(newUser);
                addDefaultCategories((int) userId);

                handler.post(() -> {
                    showLoading(false);
                    messageUtils.showSuccess(R.string.signup_success);
                    navigateToLogin();
                });

            } catch (Exception e) {
                handleError("handleFirebaseUser", getString(R.string.error_save_user), e);
                handler.post(() -> showLoading(false));
            }
        });
    }

    private void addDefaultCategories(int userId) {
        List<Category> categories = new ArrayList<>();

        // Chi tiêu categories
        categories.add(new Category(userId, "Mua sắm", Constants.CATEGORY_TYPE_EXPENSE, getString(R.string.icon_shopping_cart)));
        categories.add(new Category(userId, "Đồ ăn", Constants.CATEGORY_TYPE_EXPENSE, getString(R.string.icon_restaurant)));
        categories.add(new Category(userId, "Điện thoại", Constants.CATEGORY_TYPE_EXPENSE, getString(R.string.icon_phone)));
        categories.add(new Category(userId, "Giải trí", Constants.CATEGORY_TYPE_EXPENSE, getString(R.string.icon_games)));
        categories.add(new Category(userId, "Sức khỏe", Constants.CATEGORY_TYPE_EXPENSE, getString(R.string.icon_health_and_safety)));
        categories.add(new Category(userId, "Giáo dục", Constants.CATEGORY_TYPE_EXPENSE, getString(R.string.icon_school)));
        categories.add(new Category(userId, "Thể thao", Constants.CATEGORY_TYPE_EXPENSE, getString(R.string.icon_sports)));
        categories.add(new Category(userId, "Xã hội", Constants.CATEGORY_TYPE_EXPENSE, getString(R.string.icon_people)));
        categories.add(new Category(userId, "Vận tải", Constants.CATEGORY_TYPE_EXPENSE, getString(R.string.icon_directions_car)));
        categories.add(new Category(userId, "Quần áo", Constants.CATEGORY_TYPE_EXPENSE, getString(R.string.icon_laundry)));
        categories.add(new Category(userId, "Xe hơi", Constants.CATEGORY_TYPE_EXPENSE, getString(R.string.icon_car_repair)));

        // Thu nhập categories
        categories.add(new Category(userId, "Lương", Constants.CATEGORY_TYPE_INCOME, getString(R.string.icon_account_balance)));
        categories.add(new Category(userId, "Thưởng", Constants.CATEGORY_TYPE_INCOME, getString(R.string.icon_card_giftcard)));
        categories.add(new Category(userId, "Đầu tư", Constants.CATEGORY_TYPE_INCOME, getString(R.string.icon_trending_up)));

        try {
            appDatabase.categoryDao().insertCategories(categories);
            Log.d(TAG, "Default categories added for user: " + userId);
        } catch (Exception e) {
            Log.e(TAG, "Error adding default categories", e);
        }
    }

    private void navigateToLogin() {
        startActivity(new Intent(this, LogInActivity.class));
        finish();
    }

    private void showLoading(boolean show) {
        binding.progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.btnRegister.setEnabled(!show);
        binding.btnGoogleSignUp.setEnabled(!show);
        binding.edtFullName.setEnabled(!show);
        binding.edtEmail.setEnabled(!show);
        binding.edtPassword.setEnabled(!show);
        binding.edtConfirmPassword.setEnabled(!show);
    }

    private void setupPasswordField() {
        binding.layoutPassword.setEndIconTintList(ColorStateList.valueOf(
                ContextCompat.getColor(this, R.color.black)));
        binding.layoutPassword.setErrorIconTintList(ColorStateList.valueOf(
                ContextCompat.getColor(this, R.color.error_color)));
        binding.layoutConfirmPassword.setEndIconTintList(ColorStateList.valueOf(
                ContextCompat.getColor(this, R.color.black)));
        binding.layoutConfirmPassword.setErrorIconTintList(ColorStateList.valueOf(
                ContextCompat.getColor(this, R.color.error_color)));
    }

    private void handleError(String tag, String message, Exception e) {
        Log.e(TAG, tag + ": " + message, e);
        handler.post(() -> messageUtils.showError(message));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}