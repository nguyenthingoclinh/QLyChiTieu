package com.nhom08.qlychitieu.giao_dien.nguoi_dung;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

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
import com.nhom08.qlychitieu.tien_ich.PasswordUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

public class SignUpActivity extends AppCompatActivity {
    private static final String TAG = "SignUpActivity";
    private ActivitySignUpBinding binding;
    private CredentialManager credentialManager;
    private AppDatabase appDatabase;
    private final Executor executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean isPasswordVisible = false;
    private boolean isConfirmPasswordVisible = false;
    // Regex patterns
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[A-Z])(?=.*\\d).{8,}$");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: Started");

        initializeUI();
        initializeDatabase();
        setupCredentialManager();
        setupClickListeners();
    }

    private void initializeUI() {
        EdgeToEdge.enable(this);
        binding = ActivitySignUpBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void initializeDatabase() {
        try {
            appDatabase = ((MyApplication) getApplication()).getDatabase();
            if (appDatabase == null) {
                handleDatabaseError("Không thể khởi tạo cơ sở dữ liệu");
            }
        } catch (Exception e) {
            Log.e(TAG, "initializeDatabase: Error", e);
            handleDatabaseError("Lỗi khi khởi tạo cơ sở dữ liệu");
        }
    }

    private void handleDatabaseError(String message) {
        Log.e(TAG, "handleDatabaseError: " + message);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        finish();
    }

    private void setupCredentialManager() {
        Log.d(TAG, "setupCredentialManager: Initializing");
        credentialManager = CredentialManager.create(this);
    }

    private void setupClickListeners() {
        binding.btnRegister.setOnClickListener(v -> registerUser());
        binding.btnGoogleSignUp.setOnClickListener(v -> signInWithGoogle());
        binding.tvLoginRedirect.setOnClickListener(v -> navigateToLogin());
        binding.iconPasswordVisibility.setOnClickListener(v -> togglePasswordVisibility());
        binding.iconConfirmPasswordVisibility.setOnClickListener(v -> toggleConfirmPasswordVisibility());
    }

    private void registerUser() {
        String fullName = binding.edtFullName.getText().toString().trim();
        String email = binding.edtUsername.getText().toString().trim();
        String password = binding.edtPassword.getText().toString();
        String confirmPassword = binding.edtConfirmPassword.getText().toString();

        if (!validateInputs(fullName, email, password, confirmPassword)) {
            return;
        }

        executor.execute(() -> {
            try {
                if (isEmailExists(email)) {
                    showToast("Email đã tồn tại");
                    return;
                }

                User newUser = createUser(fullName, email, password);
                long userId = appDatabase.userDao().insertUser(newUser);
                addDefaultCategories((int) userId);

                handler.post(() -> {
                    showToast("Đăng ký thành công");
                    navigateToLogin();
                });

            } catch (Exception e) {
                Log.e(TAG, "registerUser: Error", e);
                showToast("Đã xảy ra lỗi khi đăng ký");
            }
        });
    }

    private boolean validateInputs(String fullName, String email, String password, String confirmPassword) {
        if (TextUtils.isEmpty(fullName)) {
            binding.edtFullName.setError("Vui lòng nhập họ tên");
            return false;
        }

        if (!isValidEmail(email)) {
            binding.edtUsername.setError("Email không hợp lệ");
            return false;
        }

        if (!isValidPassword(password)) {
            binding.edtPassword.setError("Mật khẩu phải có ít nhất 8 ký tự, 1 chữ hoa và 1 chữ số");
            return false;
        }

        if (!password.equals(confirmPassword)) {
            binding.edtConfirmPassword.setError("Mật khẩu không khớp");
            return false;
        }

        return true;
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
                    executor,
                    new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                        @Override
                        public void onResult(GetCredentialResponse result) {
                            handleGoogleSignInResult(result);
                        }

                        @Override
                        public void onError(@NonNull GetCredentialException e) {
                            Log.e(TAG, "Google sign in error", e);
                            showToast("Đăng nhập Google thất bại: " + e.getLocalizedMessage());
                        }
                    }
            );
        } catch (Exception e) {
            Log.e(TAG, "signInWithGoogle: Error", e);
            showToast("Lỗi khi đăng nhập bằng Google");
        }
    }

    private void handleGoogleSignInResult(GetCredentialResponse response) {
        try {
            if (!(response.getCredential() instanceof CustomCredential) ||
                    !GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL.equals(response.getCredential().getType())) {
                showToast("Không nhận được thông tin Google hợp lệ");
                return;
            }

            GoogleIdTokenCredential googleCredential = GoogleIdTokenCredential.createFrom(response.getCredential().getData());
            AuthCredential firebaseCredential = GoogleAuthProvider.getCredential(googleCredential.getIdToken(), null);

            FirebaseAuth.getInstance().signInWithCredential(firebaseCredential)
                    .addOnSuccessListener(authResult -> {
                        FirebaseUser firebaseUser = authResult.getUser();
                        if (firebaseUser == null) {
                            showToast("Không thể lấy thông tin người dùng");
                            return;
                        }

                        handleFirebaseUser(firebaseUser, googleCredential);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Firebase authentication failed", e);
                        showToast("Xác thực thất bại: " + e.getMessage());
                    });

        } catch (Exception e) {
            Log.e(TAG, "handleGoogleSignInResult: Error", e);
            showToast("Lỗi xử lý đăng nhập Google");
        }
    }

    private void handleFirebaseUser(FirebaseUser firebaseUser, GoogleIdTokenCredential googleCredential) {
        String googleId = googleCredential.getId();
        String email = firebaseUser.getEmail();
        String fullName = firebaseUser.getDisplayName();
        String photoUrl = firebaseUser.getPhotoUrl() != null ? firebaseUser.getPhotoUrl().toString() : null;

        executor.execute(() -> {
            try {
                User existingUser = appDatabase.userDao().getUserByGoogleId(googleId);
                if (existingUser != null) {
                    showToast("Tài khoản Google này đã được đăng ký");
                    navigateToLogin();
                    return;
                }

                User newUser = new User(fullName, email, null, googleId, null, photoUrl);
                long userId = appDatabase.userDao().insertUser(newUser);
                addDefaultCategories((int) userId);

                handler.post(() -> {
                    showToast("Đăng ký thành công");
                    navigateToLogin();
                });

            } catch (Exception e) {
                Log.e(TAG, "handleFirebaseUser: Error", e);
                showToast("Lỗi khi lưu thông tin người dùng");
            }
        });
    }

    private void addDefaultCategories(int userId) {
        List<Category> categories = new ArrayList<>();

        // Chi tiêu categories
        categories.add(new Category(userId, "Mua sắm", "Expense", getString(R.string.icon_shopping_cart)));
        categories.add(new Category(userId, "Đồ ăn", "Expense", getString(R.string.icon_restaurant)));
        categories.add(new Category(userId, "Điện thoại", "Expense", getString(R.string.icon_phone)));
        categories.add(new Category(userId, "Giải trí", "Expense", getString(R.string.icon_games)));
        categories.add(new Category(userId, "Sức khỏe", "Expense", getString(R.string.icon_health_and_safety)));
        categories.add(new Category(userId, "Giáo dục", "Expense", getString(R.string.icon_school)));
        categories.add(new Category(userId, "Thể thao", "Expense", getString(R.string.icon_sports)));
        categories.add(new Category(userId, "Xã hội", "Expense", getString(R.string.icon_people)));
        categories.add(new Category(userId, "Vận tải", "Expense", getString(R.string.icon_directions_car)));
        categories.add(new Category(userId, "Quần áo", "Expense", getString(R.string.icon_laundry)));
        categories.add(new Category(userId, "Xe hơi", "Expense", getString(R.string.icon_car_repair)));

        // Thu nhập categories
        categories.add(new Category(userId, "Lương", "Income", getString(R.string.icon_account_balance)));
        categories.add(new Category(userId, "Thưởng", "Income", getString(R.string.icon_card_giftcard)));
        categories.add(new Category(userId, "Đầu tư", "Income", getString(R.string.icon_trending_up)));

        try {
            appDatabase.categoryDao().insertCategories(categories);
            Log.d(TAG, "Default categories added for user: " + userId);
        } catch (Exception e) {
            Log.e(TAG, "Error adding default categories", e);
        }
    }

    private void togglePasswordVisibility() {
        isPasswordVisible = !isPasswordVisible;
        updatePasswordVisibility(
                binding.edtPassword,
                binding.iconPasswordVisibility,
                isPasswordVisible
        );
    }

    private void toggleConfirmPasswordVisibility() {
        isConfirmPasswordVisible = !isConfirmPasswordVisible;
        updatePasswordVisibility(
                binding.edtConfirmPassword,
                binding.iconConfirmPasswordVisibility,
                isConfirmPasswordVisible
        );
    }

    private void updatePasswordVisibility(EditText editText, TextView icon, boolean visible) {
        editText.setInputType(
                visible ? InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                        : (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)
        );
        icon.setText(getString(visible ? R.string.icon_visibility : R.string.icon_visibility_off));
        editText.setSelection(editText.getText().length());
    }

    private boolean isValidEmail(String email) {
        return !TextUtils.isEmpty(email) && EMAIL_PATTERN.matcher(email).matches();
    }

    private boolean isValidPassword(String password) {
        return !TextUtils.isEmpty(password) && PASSWORD_PATTERN.matcher(password).matches();
    }

    private void navigateToLogin() {
        startActivity(new Intent(this, LogInActivity.class));
        finish();
    }

    private void showToast(String message) {
        handler.post(() -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}