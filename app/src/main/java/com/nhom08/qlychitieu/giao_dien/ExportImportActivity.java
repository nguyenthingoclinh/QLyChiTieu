package com.nhom08.qlychitieu.giao_dien;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.nhom08.qlychitieu.MyApplication;
import com.nhom08.qlychitieu.R;
import com.nhom08.qlychitieu.csdl.AppDatabase;
import com.nhom08.qlychitieu.databinding.ActivityExportImportBinding;
import com.nhom08.qlychitieu.tien_ich.ExportImportHelper;
import com.nhom08.qlychitieu.tien_ich.MessageUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;

public class ExportImportActivity extends AppCompatActivity {

    private ActivityExportImportBinding binding;
    private AppDatabase database;
    private ExecutorService executorService;
    private int currentUserId;
    private String fileName;
    private MessageUtils messageUtils;

    // Launcher để chọn file khi nhập dữ liệu
    private final ActivityResultLauncher<Intent> filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        importDataFromUri(uri);
                    }
                }
            });

    // Launcher để chọn vị trí lưu file khi xuất dữ liệu
    private final ActivityResultLauncher<Intent> saveFileLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri destinationUri = result.getData().getData();
                    if (destinationUri != null) {
                        saveToSelectedLocation(destinationUri);
                    }
                } else {
                    messageUtils.showInfo("Hủy xuất dữ liệu");
                }
            });

    // Dữ liệu tạm để lưu trữ khi xuất
    private File tempExportFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityExportImportBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Khởi tạo MessageUtils
        messageUtils = new MessageUtils(this);

        // Lấy thực thể từ application
        MyApplication app = (MyApplication) getApplication();
        database = app.getDatabase();
        executorService = app.getExecutorService();
        currentUserId = app.getCurrentUserId();

        // Thiết lập nút quay lại
        binding.tvBack.setOnClickListener(v -> finish());

        // Thiết lập màu thanh trạng thái
        getWindow().setStatusBarColor(getResources().getColor(R.color.colorPrimary, getTheme()));

        // Thiết lập nút xuất dữ liệu
        binding.btnExport.setOnClickListener(v -> exportData());

        // Thiết lập nút nhập dữ liệu
        binding.btnImport.setOnClickListener(v -> showImportConfirmation());
    }

    /**
     * Xuất dữ liệu ra file JSON
     */
    private void exportData() {
        // Tạo tên file với timestamp
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        fileName = "qlychitieu_data_" + sdf.format(new Date()) + ".json";

        // Sử dụng MaterialAlertDialogBuilder thay vì ProgressDialog
        AlertDialog progressDialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Đang tạo dữ liệu xuất")
                .setMessage("Vui lòng đợi...")
                .setCancelable(false)
                .create();
        progressDialog.show();

        executorService.execute(() -> {
            try {
                // Xuất dữ liệu nhưng chỉ tạo tạm trong bộ nhớ
                tempExportFile = ExportImportHelper.exportData(this, database, currentUserId);

                runOnUiThread(() -> {
                    progressDialog.dismiss();

                    // Hiển thị dialog cho người dùng chọn lựa
                    new MaterialAlertDialogBuilder(this)
                            .setTitle("Xuất dữ liệu thành công")
                            .setMessage("Bạn muốn thực hiện thao tác nào với file dữ liệu?")
                            .setPositiveButton("Chia sẻ", (dialog, which) -> shareFile(tempExportFile))
                            .setNegativeButton("Lưu vào vị trí khác", (dialog, which) -> openSaveFileDialog())
                            .setNeutralButton("Hủy", null)
                            .show();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    messageUtils.showError("Lỗi khi xuất dữ liệu: " + e.getMessage());
                });
            }
        });
    }

    /**
     * Mở dialog để chọn vị trí lưu file
     */
    private void openSaveFileDialog() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        intent.putExtra(Intent.EXTRA_TITLE, fileName);

        try {
            saveFileLauncher.launch(intent);
        } catch (Exception e) {
            messageUtils.showError("Không thể mở trình chọn vị trí lưu: " + e.getMessage());
        }
    }

    /**
     * Lưu dữ liệu vào vị trí đã chọn
     */
    private void saveToSelectedLocation(Uri destinationUri) {
        AlertDialog progressDialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Đang lưu dữ liệu")
                .setMessage("Vui lòng đợi...")
                .setCancelable(false)
                .create();
        progressDialog.show();

        executorService.execute(() -> {
            try (InputStream inputStream = new FileInputStream(tempExportFile);
                 OutputStream outputStream = getContentResolver().openOutputStream(destinationUri)) {

                if (outputStream == null) {
                    throw new IOException("Không thể mở file để ghi");
                }

                // Copy dữ liệu từ file tạm sang vị trí đã chọn
                byte[] buffer = new byte[4096];
                int bytesRead;

                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }

                outputStream.flush();

                // Hiển thị thông báo thành công
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    messageUtils.showSuccess("Đã lưu dữ liệu thành công");
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    messageUtils.showError("Lỗi khi lưu dữ liệu: " + e.getMessage());
                });
            }
        });
    }

    /**
     * Chia sẻ file đã xuất
     */
    private void shareFile(File file) {
        try {
            Uri fileUri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    file);

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("application/json");
            shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(Intent.createChooser(shareIntent, "Chia sẻ dữ liệu"));
        } catch (Exception e) {
            messageUtils.showError("Lỗi khi chia sẻ file: " + e.getMessage());
        }
    }

    /**
     * Hiển thị hộp thoại xác nhận trước khi nhập dữ liệu
     */
    private void showImportConfirmation() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Xác nhận nhập dữ liệu")
                .setMessage("Cảnh báo: Tất cả dữ liệu hiện tại sẽ bị xóa và thay thế bằng dữ liệu từ file nhập. Bạn có chắc chắn muốn tiếp tục?")
                .setPositiveButton("Tiếp tục", (dialog, which) -> openFilePicker())
                .setNegativeButton("Hủy", null)
                .show();
    }

    /**
     * Mở trình chọn file để nhập dữ liệu
     */
    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");

        try {
            filePickerLauncher.launch(intent);
        } catch (Exception e) {
            messageUtils.showError("Không thể mở trình chọn file: " + e.getMessage());
        }
    }

    /**
     * Nhập dữ liệu từ URI đã chọn
     */
    private void importDataFromUri(Uri uri) {
        // Sử dụng MaterialAlertDialogBuilder thay vì ProgressDialog
        AlertDialog progressDialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Đang nhập dữ liệu")
                .setMessage("Vui lòng đợi...")
                .setCancelable(false)
                .create();
        progressDialog.show();

        executorService.execute(() -> {
            File tempFile = null;
            try {
                // Sao chép file từ Uri vào thư mục tạm thời
                tempFile = copyUriToTempFile(uri);

                // Nhập dữ liệu từ file
                boolean success = ExportImportHelper.importData(this, database, tempFile, currentUserId);

                final File finalTempFile = tempFile;  // Tạo bản sao final để sử dụng trong runOnUiThread
                runOnUiThread(() -> {
                    progressDialog.dismiss();

                    // Xóa file tạm
                    if (finalTempFile.exists()) {
                        boolean deleted = finalTempFile.delete();
                        if (!deleted) {
                            // Chỉ ghi log, không cần xử lý đặc biệt nếu không xóa được
                            finalTempFile.deleteOnExit();
                        }
                    }

                    if (success) {
                        new MaterialAlertDialogBuilder(this)
                                .setTitle("Nhập dữ liệu thành công")
                                .setMessage("Dữ liệu đã được nhập thành công vào ứng dụng.")
                                .setPositiveButton("OK", (dialog, which) -> {
                                    // Thông báo thành công và đóng màn hình
                                    messageUtils.showSuccess("Nhập dữ liệu hoàn tất");
                                    finish();
                                })
                                .setCancelable(false)
                                .show();
                    } else {
                        messageUtils.showError("Không thể nhập dữ liệu");
                    }
                });
            } catch (Exception e) {
                // Xóa file tạm nếu có lỗi
                if (tempFile != null && tempFile.exists()) {
                    tempFile.delete();
                }

                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    messageUtils.showError("Lỗi khi nhập dữ liệu: " + e.getMessage());
                });
            }
        });
    }

    /**
     * Sao chép dữ liệu từ Uri vào file tạm thời
     */
    private File copyUriToTempFile(Uri uri) throws IOException {
        File tempFile = File.createTempFile("import_", ".json", getCacheDir());

        try (InputStream inputStream = getContentResolver().openInputStream(uri);
             FileOutputStream outputStream = new FileOutputStream(tempFile)) {

            // Kiểm tra inputStream không null
            if (inputStream == null) {
                throw new IOException("Không thể mở file để đọc");
            }

            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            outputStream.flush();
        }

        return tempFile;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Dọn dẹp file tạm nếu còn tồn tại
        if (tempExportFile != null && tempExportFile.exists()) {
            tempExportFile.delete();
        }
    }
}