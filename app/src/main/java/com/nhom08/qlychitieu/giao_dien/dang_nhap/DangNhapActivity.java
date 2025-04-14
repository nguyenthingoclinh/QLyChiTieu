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

import com.nhom08.qlychitieu.csdl.AppDatabase;
import com.nhom08.qlychitieu.csdl.DatabaseClient;
import com.nhom08.qlychitieu.R;
import com.nhom08.qlychitieu.giao_dien.man_hinh_chinh.MainActivity;
import com.nhom08.qlychitieu.mo_hinh.User;
import com.nhom08.qlychitieu.truy_van.UserDAO;
import com.nhom08.qlychitieu.tien_ich.PasswordUtil;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class DangNhapActivity extends AppCompatActivity {

    private EditText edtUsername, edtPassword;
    private Button btnLogin;
    private Button btnGoogleLogin;
    private CredentialManager credentialManager;
    private final Executor executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private AppDatabase appDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.dang_nhap);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        // Khởi tạo DatabaseClient và lấy AppDatabase một lần
        appDatabase = DatabaseClient.getInstance(getApplicationContext()).getAppDatabase();
        mapping();

        credentialManager = CredentialManager.create(this);

        btnLogin.setOnClickListener(v -> loginUser());
        btnGoogleLogin.setOnClickListener(v -> signInWithGoogle());
        findViewById(R.id.btnViewRegister).setOnClickListener(v -> {
            startActivity(new Intent(DangNhapActivity.this, DangKyActivity.class));
            finish();
        });
    }

    private void loginUser() {
        final String email = edtUsername.getText().toString().trim();
        final String password = edtPassword.getText().toString().trim();
        String hashPassword = PasswordUtil.hashPassword(password);

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        executor.execute(() -> {
            UserDAO userDAO = appDatabase.userDao();
            final User existingUser = userDAO.getUserByEmail(email);

            handler.post(() -> {
                if (existingUser == null) {
                    // Trường hợp email không tồn tại
                    Toast.makeText(DangNhapActivity.this, "Tài khoản không tồn tại", Toast.LENGTH_SHORT).show();
                } else {
                    // Bước 2: Nếu email tồn tại, kiểm tra mật khẩu
                    if (!PasswordUtil.checkPassword(password, existingUser.getPassword())) {
                        Toast.makeText(DangNhapActivity.this, "Sai mật khẩu", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(DangNhapActivity.this, "Đăng nhập thành công", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(DangNhapActivity.this, MainActivity.class));
                        finish();
                    }
                }
            });
        });
    }

    private void signInWithGoogle() {
        GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                .setServerClientId("35380319939-n031rkvaire7qniekd3k5afakm7r145m.apps.googleusercontent.com") // Thay bằng Server Client ID của bạn
                .setFilterByAuthorizedAccounts(false)
                .setAutoSelectEnabled(false)
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
                        Toast.makeText(DangNhapActivity.this, "Đăng nhập bằng Google thất bại: " + e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void handleGoogleSignInResult(GetCredentialResponse response) {
        if (response.getCredential() instanceof CustomCredential &&
                response.getCredential().getType().equals(GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL)) {
            GoogleIdTokenCredential googleCredential = GoogleIdTokenCredential.createFrom(response.getCredential().getData());
            final String googleId = googleCredential.getId();

            executor.execute(() -> {
                UserDAO userDAO = appDatabase.userDao();
                final User existingUser = userDAO.getUserByGoogleId(googleId);

                handler.post(() -> {
                    if (existingUser == null) {
                        Toast.makeText(DangNhapActivity.this, "Chưa có tài khoản, vui lòng tạo tài khoản", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(DangNhapActivity.this, DangKyActivity.class));
                        finish();
                    } else {
                        Toast.makeText(DangNhapActivity.this, "Đăng nhập bằng Google thành công", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(DangNhapActivity.this, MainActivity.class));
                        finish();
                    }
                });
            });
        } else {
            Toast.makeText(this, "Không nhận được thông tin Google", Toast.LENGTH_SHORT).show();
        }
    }


    // Ánh xạ view từ XML
    private void mapping() {
        edtUsername = findViewById(R.id.edtUsername);
        if (edtUsername == null)
            throw new IllegalStateException("edtUsername không có trong layout");

        edtPassword = findViewById(R.id.edtPassword);
        if (edtPassword == null)
            throw new IllegalStateException("edtPassword không có trong layout");

        btnLogin = findViewById(R.id.btnLogin);
        if (btnLogin == null) throw new IllegalStateException("btnLogin không có trong layout");

        btnGoogleLogin = findViewById(R.id.btnGoogleLogin);
        if (btnGoogleLogin == null)
            throw new IllegalStateException("btnGoogleLogin không có trong layout");
    }
}