package com.nhom08.qlychitieu.giao_dien.dang_nhap;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;
import android.text.InputType;

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
import com.nhom08.qlychitieu.csdl.AppDatabase;
import com.nhom08.qlychitieu.mo_hinh.User;
import com.nhom08.qlychitieu.truy_van.UserDAO;
import com.nhom08.qlychitieu.tien_ich.PasswordUtil;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class SignInActivity extends AppCompatActivity {
    private static final String TAG = "SignInActivity";

    private EditText edtFullName, edtUsername, edtPassword, edtConfirmPassword;
    private Button btnRegister, btnGoogleRegister;
    private TextView iconPasswordVisibility, iconConfirmPasswordVisibility;
    private CredentialManager credentialManager;
    private final Executor executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private AppDatabase appDatabase;
    private boolean isPasswordVisible = false;
    private boolean isConfirmPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: Started");

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sign_in);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Lấy AppDatabase từ MyApplication
        Log.d(TAG, "onCreate: Getting AppDatabase from MyApplication");
        try {
            appDatabase = ((MyApplication) getApplication()).getDatabase();
            if (appDatabase == null) {
                Log.e(TAG, "onCreate: AppDatabase is null");
                Toast.makeText(this, "Không thể khởi tạo cơ sở dữ liệu", Toast.LENGTH_LONG).show();
                finish();
                return;
            }
        } catch (Exception e) {
            Log.e(TAG, "onCreate: Error while getting AppDatabase", e);
            Toast.makeText(this, "Không thể khởi tạo cơ sở dữ liệu", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        mapping();

        // Khởi tạo CredentialManager
        Log.d(TAG, "onCreate: Initializing CredentialManager");
        credentialManager = CredentialManager.create(this);

        btnRegister.setOnClickListener(v -> registerUser());
        btnGoogleRegister.setOnClickListener(v -> signInWithGoogle());
        findViewById(R.id.tvLoginRedirect).setOnClickListener(v -> {
            Log.d(TAG, "onCreate: tvLoginRedirect clicked, starting LoginActivity");
            startActivity(new Intent(SignInActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void registerUser() {
        Log.d(TAG, "registerUser: Started");
        final String fullName = edtFullName.getText().toString().trim();
        final String email = edtUsername.getText().toString().trim();
        final String password = edtPassword.getText().toString().trim();
        final String confirmPassword = edtConfirmPassword.getText().toString().trim();

        if (fullName.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Log.w(TAG, "registerUser: Missing required fields");
            Toast.makeText(this, "Vui lòng nhập đầy TOW đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Log.w(TAG, "registerUser: Passwords do not match");
            Toast.makeText(this, "Mật khẩu không khớp", Toast.LENGTH_SHORT).show();
            return;
        }

        executor.execute(() -> {
            Log.d(TAG, "registerUser: Checking if email exists: " + email);
            try {
                UserDAO userDAO = appDatabase.userDao();
                final User existingUser = userDAO.getUserByEmail(email);

                handler.post(() -> {
                    if (existingUser != null) {
                        Log.w(TAG, "registerUser: Email already exists: " + email);
                        Toast.makeText(SignInActivity.this, "Email đã tồn tại", Toast.LENGTH_SHORT).show();
                    } else {
                        final String hashedPassword = PasswordUtil.hashPassword(password);
                        final User newUser = new User(fullName, email, hashedPassword, null, null, null);
                        executor.execute(() -> {
                            Log.d(TAG, "registerUser: Inserting new user into database");
                            userDAO.insertUser(newUser);
                            handler.post(() -> {
                                Log.d(TAG, "registerUser: Registration successful, starting LoginActivity");
                                Toast.makeText(SignInActivity.this, "Đăng ký thành công", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(SignInActivity.this, LoginActivity.class));
                                finish();
                            });
                        });
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "registerUser: Error while registering user", e);
                handler.post(() -> {
                    Toast.makeText(SignInActivity.this, "Đã xảy ra lỗi khi đăng ký", Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    // Xử lý show/hide password cho trường Password
    public void togglePasswordVisibility(View view) {
        isPasswordVisible = !isPasswordVisible;
        if (isPasswordVisible) {
            // Hiển thị mật khẩu
            edtPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            iconPasswordVisibility.setText(getString(R.string.icon_visibility));
        } else {
            // Ẩn mật khẩu
            edtPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            iconPasswordVisibility.setText(getString(R.string.icon_visibility_off));
        }
        // Di chuyển con trỏ về cuối văn bản
        edtPassword.setSelection(edtPassword.getText().length());
    }

    // Xử lý show/hide password cho trường Confirm Password
    public void toggleConfirmPasswordVisibility(View view) {
        isConfirmPasswordVisible = !isConfirmPasswordVisible;
        if (isConfirmPasswordVisible) {
            // Hiển thị mật khẩu
            edtConfirmPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            iconConfirmPasswordVisibility.setText(getString(R.string.icon_visibility));
        } else {
            // Ẩn mật khẩu
            edtConfirmPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            iconConfirmPasswordVisibility.setText(getString(R.string.icon_visibility_off));
        }
        // Di chuyển con trỏ về cuối văn bản
        edtConfirmPassword.setSelection(edtConfirmPassword.getText().length());
    }

    // Đăng ký bằng Google
    private void signInWithGoogle() {
        Log.d(TAG, "signInWithGoogle: Started");
        try {
            GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                    .setServerClientId("35380319939-n031rkvaire7qniekd3k5afakm7r145m.apps.googleusercontent.com")
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
                    new CredentialManagerCallback<>() {
                        @Override
                        public void onResult(GetCredentialResponse result) {
                            Log.d(TAG, "signInWithGoogle: Credential received, handling result");
                            handleGoogleSignInResult(result);
                        }

                        @Override
                        public void onError(@NonNull GetCredentialException e) {
                            Log.e(TAG, "signInWithGoogle: Error while signing in with Google", e);
                            Toast.makeText(SignInActivity.this, "Đăng ký bằng Google thất bại: " + e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
            );
        } catch (Exception e) {
            Log.e(TAG, "signInWithGoogle: Unexpected error", e);
            Toast.makeText(SignInActivity.this, "Đã xảy ra lỗi khi đăng ký bằng Google", Toast.LENGTH_LONG).show();
        }
    }

    private void handleGoogleSignInResult(GetCredentialResponse response) {
        Log.d(TAG, "handleGoogleSignInResult: Started");
        try {
            if (response.getCredential() instanceof CustomCredential &&
                    response.getCredential().getType().equals(GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL)) {
                GoogleIdTokenCredential googleCredential = GoogleIdTokenCredential.createFrom(response.getCredential().getData());
                String idToken = googleCredential.getIdToken();
                Log.d(TAG, "handleGoogleSignInResult: ID Token received");

                // Tạo FirebaseCredential từ idToken
                AuthCredential firebaseCredential = GoogleAuthProvider.getCredential(idToken, null);
                Log.d(TAG, "handleGoogleSignInResult: Firebase credential created");

                // Đăng nhập vào Firebase để lấy thông tin người dùng đầy đủ
                FirebaseAuth.getInstance().signInWithCredential(firebaseCredential)
                        .addOnCompleteListener(this, task -> {
                            if (task.isSuccessful()) {
                                Log.d(TAG, "handleGoogleSignInResult: Firebase sign-in successful");
                                // Lấy thông tin người dùng từ Firebase User
                                FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
                                if (firebaseUser == null) {
                                    Log.e(TAG, "handleGoogleSignInResult: FirebaseUser is null");
                                    Toast.makeText(SignInActivity.this, "Không thể lấy thông tin người dùng từ Firebase", Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                final String googleId = googleCredential.getId();
                                final String email = firebaseUser.getEmail() != null ? firebaseUser.getEmail() : "";
                                final String fullName = firebaseUser.getDisplayName() != null ? firebaseUser.getDisplayName() :
                                        (googleCredential.getDisplayName() != null ? googleCredential.getDisplayName() : "Unknown");
                                final String photoUrl = firebaseUser.getPhotoUrl() != null ?
                                        firebaseUser.getPhotoUrl().toString() :
                                        (googleCredential.getProfilePictureUri() != null ?
                                                googleCredential.getProfilePictureUri().toString() : null);
                                Log.d(TAG, "handleGoogleSignInResult: User info - Google ID: " + googleId + ", Email: " + email + ", FullName: " + fullName);

                                executor.execute(() -> {
                                    Log.d(TAG, "handleGoogleSignInResult: Checking if user exists in database");
                                    try {
                                        UserDAO userDAO = appDatabase.userDao();
                                        final User existingUser = userDAO.getUserByGoogleId(googleId);

                                        handler.post(() -> {
                                            if (existingUser != null) {
                                                Log.w(TAG, "handleGoogleSignInResult: User already exists with Google ID: " + googleId);
                                                Toast.makeText(SignInActivity.this, "Tài khoản này đã được đăng ký", Toast.LENGTH_SHORT).show();
                                                startActivity(new Intent(SignInActivity.this, LoginActivity.class));
                                                finish();
                                            } else {
                                                Log.d(TAG, "handleGoogleSignInResult: Creating new user");
                                                final User newUser = new User(fullName, email, null, googleId, null, photoUrl);
                                                executor.execute(() -> {
                                                    Log.d(TAG, "handleGoogleSignInResult: Inserting new user into database");
                                                    userDAO.insertUser(newUser);
                                                    handler.post(() -> {
                                                        Log.d(TAG, "handleGoogleSignInResult: Registration successful, starting LoginActivity");
                                                        Toast.makeText(SignInActivity.this, "Đăng ký tài khoản thành công", Toast.LENGTH_SHORT).show();
                                                        startActivity(new Intent(SignInActivity.this, LoginActivity.class));
                                                        finish();
                                                    });
                                                });
                                            }
                                        });
                                    } catch (Exception e) {
                                        Log.e(TAG, "handleGoogleSignInResult: Error while querying or inserting user", e);
                                        handler.post(() -> {
                                            Toast.makeText(SignInActivity.this, "Đã xảy ra lỗi khi lưu thông tin người dùng", Toast.LENGTH_LONG).show();
                                        });
                                    }
                                });
                            } else {
                                Log.e(TAG, "handleGoogleSignInResult: Firebase sign-in failed");
                                Exception exception = task.getException();
                                Toast.makeText(SignInActivity.this, "Xác thực Firebase thất bại: " +
                                                (exception != null ? exception.getMessage() : "Unknown error"),
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
            } else {
                Log.w(TAG, "handleGoogleSignInResult: No Google credential received");
                Toast.makeText(this, "Không nhận được thông tin Google", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "handleGoogleSignInResult: Unexpected error", e);
            Toast.makeText(this, "Đã xảy ra lỗi khi xử lý thông tin Google", Toast.LENGTH_LONG).show();
        }
    }

    // Ánh xạ view từ XML
    private void mapping() {
        Log.d(TAG, "mapping: Started");
        try {
            edtFullName = findViewById(R.id.edtFullName);
            if (edtFullName == null) throw new IllegalStateException("edtFullName không có trong layout");

            edtUsername = findViewById(R.id.edtUsername);
            if (edtUsername == null) throw new IllegalStateException("edtUsername không có trong layout");

            edtPassword = findViewById(R.id.edtPassword);
            if (edtPassword == null) throw new IllegalStateException("edtPassword không có trong layout");

            edtConfirmPassword = findViewById(R.id.edtConfirmPassword);
            if (edtConfirmPassword == null) throw new IllegalStateException("edtConfirmPassword không có trong layout");

            btnRegister = findViewById(R.id.btnRegister);
            if (btnRegister == null) throw new IllegalStateException("btnRegister không có trong layout");

            btnGoogleRegister = findViewById(R.id.btnGoogleRegister);
            if (btnGoogleRegister == null) throw new IllegalStateException("btnGoogleRegister không có trong layout");

            iconPasswordVisibility = findViewById(R.id.iconPasswordVisibility);
            if (iconPasswordVisibility == null) throw new IllegalStateException("iconPasswordVisibility không có trong layout");

            iconConfirmPasswordVisibility = findViewById(R.id.iconConfirmPasswordVisibility);
            if (iconConfirmPasswordVisibility == null) throw new IllegalStateException("iconConfirmPasswordVisibility không có trong layout");

            Log.d(TAG, "mapping: Completed successfully");
        } catch (Exception e) {
            Log.e(TAG, "mapping: Error while mapping views", e);
            Toast.makeText(this, "Đã xảy ra lỗi khi khởi tạo giao diện", Toast.LENGTH_LONG).show();
            finish();
        }
    }
}