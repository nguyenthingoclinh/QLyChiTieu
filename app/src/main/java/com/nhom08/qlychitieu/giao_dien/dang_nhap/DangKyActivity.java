package com.nhom08.qlychitieu.giao_dien.dang_nhap;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.EditText;
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

import com.nhom08.qlychitieu.R;
import com.nhom08.qlychitieu.csdl.AppDatabase;
import com.nhom08.qlychitieu.csdl.DatabaseClient;
import com.nhom08.qlychitieu.mo_hinh.User;
import com.nhom08.qlychitieu.truy_van.UserDAO;
import com.nhom08.qlychitieu.tien_ich.PasswordUtil;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class DangKyActivity extends AppCompatActivity {
    private EditText edtFullName, edtUsername, edtPassword, edtConfirmPassword;
    private Button btnRegister, btnGoogleRegister;
    private CredentialManager credentialManager;
    private final Executor executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private AppDatabase appDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.dang_ky);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Khởi tạo DatabaseClient và lấy AppDatabase một lần
        appDatabase = DatabaseClient.getInstance(getApplicationContext()).getAppDatabase();
        mapping();

        // Khởi tạo CredentialManager
        credentialManager = CredentialManager.create(this);

        btnRegister.setOnClickListener(v -> registerUser());
        btnGoogleRegister.setOnClickListener(v -> signInWithGoogle());
        findViewById(R.id.tvLoginRedirect).setOnClickListener(v -> {
            startActivity(new Intent(DangKyActivity.this, DangNhapActivity.class));
            finish();
        });
    }

    private void registerUser() {
        final String fullName = edtFullName.getText().toString().trim();
        final String email = edtUsername.getText().toString().trim();
        final String password = edtPassword.getText().toString().trim();
        final String confirmPassword = edtConfirmPassword.getText().toString().trim();

        if (fullName.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Mật khẩu không khớp", Toast.LENGTH_SHORT).show();
            return;
        }

        executor.execute(() -> {
            UserDAO userDAO = appDatabase.userDao();
            final User existingUser = userDAO.getUserByEmail(email);

            handler.post(() -> {
                if (existingUser != null) {
                    Toast.makeText(DangKyActivity.this, "Email đã tồn tại", Toast.LENGTH_SHORT).show();
                } else {
                    final String hashedPassword = PasswordUtil.hashPassword(password);
                    //Lưu ý: Nhớ khai báo theo đúng thứ tự của constructor trong file User.java
                    final User newUser = new User(fullName, email, hashedPassword, null, null, null);
                    executor.execute(() -> {
                        userDAO.insertUser(newUser);
                        handler.post(() -> {
                            Toast.makeText(DangKyActivity.this, "Đăng ký thành công", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(DangKyActivity.this, DangNhapActivity.class));
                            finish();
                        });
                    });
                }
            });
        });
    }

    //Đăng ký bằng Google
    private void signInWithGoogle() {
        GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                .setServerClientId("35380319939-n031rkvaire7qniekd3k5afakm7r145m.apps.googleusercontent.com") // Thay bằng Server Client ID của bạn
                .setFilterByAuthorizedAccounts(false)
                .setAutoSelectEnabled(true)
                .build();

        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build();

        credentialManager.getCredentialAsync(
                this,
                request,
                null, // CancellationSignal
                executor,
                new CredentialManagerCallback<>() {
                    @Override
                    public void onResult(GetCredentialResponse result) {
                        handleGoogleSignInResult(result);
                    }

                    @Override
                    public void onError(@NonNull GetCredentialException e) {
                        Toast.makeText(DangKyActivity.this, "Đăng ký bằng Google thất bại: " + e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void handleGoogleSignInResult(GetCredentialResponse response) {
        if (response.getCredential() instanceof CustomCredential &&
                response.getCredential().getType().equals(GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL)) {
            GoogleIdTokenCredential googleCredential = GoogleIdTokenCredential.createFrom(response.getCredential().getData());
            String idToken = googleCredential.getIdToken();

            // Tạo FirebaseCredential từ idToken
            AuthCredential firebaseCredential = GoogleAuthProvider.getCredential(idToken, null);

            // Đăng nhập vào Firebase để lấy thông tin người dùng đầy đủ
            FirebaseAuth.getInstance().signInWithCredential(firebaseCredential)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            // Lấy thông tin người dùng từ Firebase User
                            FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
                            final String googleId = googleCredential.getId();
                            final String email = firebaseUser.getEmail() != null ? firebaseUser.getEmail() : "";
                            final String fullName = firebaseUser.getDisplayName() != null ? firebaseUser.getDisplayName() :
                                    googleCredential.getDisplayName();
                            final String photoUrl = firebaseUser.getPhotoUrl() != null ?
                                    firebaseUser.getPhotoUrl().toString() :
                                    (googleCredential.getProfilePictureUri() != null ?
                                            googleCredential.getProfilePictureUri().toString() : null);

                            executor.execute(() -> {
                                UserDAO userDAO = appDatabase.userDao();
                                final User existingUser = userDAO.getUserByGoogleId(googleId);

                                handler.post(() -> {
                                    if (existingUser != null) {
                                        Toast.makeText(DangKyActivity.this, "Tài khoản này đã được đăng ký", Toast.LENGTH_SHORT).show();
                                        // Chuyển đến đăng nhập
                                        startActivity(new Intent(DangKyActivity.this, DangNhapActivity.class));
                                        finish();
                                    } else {
                                        // Tạo user mới với thông tin đầy đủ từ Firebase
                                        final User newUser = new User(fullName, email, null, googleId, null, photoUrl);
                                        executor.execute(() -> {
                                            userDAO.insertUser(newUser);
                                            handler.post(() -> {
                                                Toast.makeText(DangKyActivity.this, "Đăng ký tài khoản thành công", Toast.LENGTH_SHORT).show();
                                                startActivity(new Intent(DangKyActivity.this, DangNhapActivity.class));
                                                finish();
                                            });
                                        });
                                    }
                                });
                            });
                        } else {
                            // Xử lý lỗi khi xác thực Firebase thất bại
                            Exception exception = task.getException();
                            Toast.makeText(DangKyActivity.this, "Xác thực Firebase thất bại: " +
                                            (exception != null ? exception.getMessage() : "Unknown error"),
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            Toast.makeText(this, "Không nhận được thông tin Google", Toast.LENGTH_SHORT).show();
        }
    }

    // Ánh xạ view từ XML
    private void mapping() {
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
    }
}