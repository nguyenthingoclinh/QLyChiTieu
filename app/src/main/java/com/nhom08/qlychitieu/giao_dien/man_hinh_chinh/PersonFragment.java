package com.nhom08.qlychitieu.giao_dien.man_hinh_chinh;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.nhom08.qlychitieu.MyApplication;
import com.nhom08.qlychitieu.R;
import com.nhom08.qlychitieu.databinding.FragmentPersonBinding;
import com.nhom08.qlychitieu.giao_dien.SettingActivity;
import com.nhom08.qlychitieu.giao_dien.danh_muc.CategoryActivity;
import com.nhom08.qlychitieu.giao_dien.nguoi_dung.LogInActivity;
import com.nhom08.qlychitieu.giao_dien.nguoi_dung.UserProfileFragment;
import com.nhom08.qlychitieu.giao_dien.thong_bao.NotificationSettingsActivity;
import com.nhom08.qlychitieu.giao_dien.ExportImportActivity;
import com.nhom08.qlychitieu.mo_hinh.User;
import com.nhom08.qlychitieu.tien_ich.MessageUtils;

import java.io.File;

/**
 * Fragment quản lý thông tin cá nhân và cài đặt
 * - Hiển thị thông tin người dùng hiện tại
 * - Cung cấp các tùy chọn cài đặt như: quản lý danh mục, cài đặt chung, thông báo
 * - Cho phép xuất/nhập dữ liệu
 * - Cho phép đăng xuất khỏi ứng dụng
 */
public class PersonFragment extends Fragment {
    // View binding cho truy cập các thành phần giao diện
    private FragmentPersonBinding binding;

    // Tiện ích hiển thị thông báo
    private MessageUtils messageUtils;

    // Đối tượng Application toàn cục
    private MyApplication app;

    /**
     * Được gọi khi fragment được tạo
     * Khởi tạo các đối tượng cần thiết
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        app = MyApplication.getInstance();
        messageUtils = new MessageUtils(requireContext());
    }

    /**
     * Tạo và trả về view cho fragment
     * Sử dụng view binding để inflate layout
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentPersonBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    /**
     * Được gọi sau khi view được tạo
     * Thiết lập các thành phần giao diện và cập nhật thông tin người dùng
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        thietLapThanhPhanGiaoDien();
        capNhatThongTinNguoiDung();
    }

    /**
     * Thiết lập các thành phần giao diện và đăng ký sự kiện click
     * Mỗi menu item sẽ mở một activity tương ứng khi được nhấn
     */
    private void thietLapThanhPhanGiaoDien() {
        binding.cardUserProfile.setOnClickListener(v -> openUserProfile());
        // Thiết lập sự kiện click cho menu quản lý danh mục
        binding.menuCategorySetting.setOnClickListener(v -> moQuanLyDanhMuc());

        // Thiết lập sự kiện click cho menu cài đặt chung
        binding.menuSetting.setOnClickListener(v -> moCaiDat());

        // Thiết lập sự kiện click cho menu thông báo
        binding.menuAlert.setOnClickListener(v -> moCaiDatThongBao());

        // Thiết lập sự kiện click cho menu xuất/nhập dữ liệu
        binding.menuExport.setOnClickListener(v -> moXuatNhapDuLieu());

        // Thiết lập sự kiện click cho menu đăng xuất
        binding.menuLogout.setOnClickListener(v -> xuLyDangXuat());
    }

    private void openUserProfile() {
        Fragment userProfileFragment = new UserProfileFragment();
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.frame_layout, userProfileFragment)
                .addToBackStack(null)
                .commit();
    }
    /**
     * Cập nhật thông tin người dùng hiện tại lên giao diện
     * Nếu không có người dùng đăng nhập, chuyển hướng đến màn hình đăng nhập
     */
    private void capNhatThongTinNguoiDung() {
        User currentUser = app.getCurrentUser();
        if (currentUser != null) {
            binding.tvUsername.setText(currentUser.getFullName());
            binding.tvUserEmail.setText(currentUser.getEmail());
            String avatarPath = currentUser.getAvatarPath();
            if (avatarPath != null && !avatarPath.isEmpty()) {
                File avatarFile = new File(avatarPath);
                if (avatarFile.exists()) {
                    binding.imgAvatar.setImageURI(Uri.fromFile(avatarFile));
                } else {
                    binding.imgAvatar.setImageResource(R.drawable.ic_avatar_default);
                }
            } else {
                binding.imgAvatar.setImageResource(R.drawable.ic_avatar_default);
            }

        } else {

            chuyenDenDangNhap();
        }
    }

    /**
     * Mở màn hình quản lý danh mục
     * Cho phép người dùng thêm, sửa, xóa các danh mục chi tiêu và thu nhập
     */
    private void moQuanLyDanhMuc() {
        startActivity(new Intent(requireContext(), CategoryActivity.class));
    }

    /**
     * Mở màn hình cài đặt chung
     * Cho phép người dùng điều chỉnh các cài đặt như ngôn ngữ, đơn vị tiền tệ,...
     */
    private void moCaiDat() {
        startActivity(new Intent(requireContext(), SettingActivity.class));
    }

    /**
     * Mở màn hình cài đặt thông báo
     * Cho phép người dùng tùy chỉnh các thông báo nhắc nhở
     */
    private void moCaiDatThongBao() {
        startActivity(new Intent(requireContext(), NotificationSettingsActivity.class));
    }

    /**
     * Mở màn hình xuất/nhập dữ liệu
     * Cho phép người dùng sao lưu và khôi phục dữ liệu
     */
    private void moXuatNhapDuLieu() {
        startActivity(new Intent(requireContext(), ExportImportActivity.class));
    }

    /**
     * Xử lý đăng xuất khỏi ứng dụng
     * Xóa thông tin phiên đăng nhập và chuyển hướng đến màn hình đăng nhập
     */
    private void xuLyDangXuat() {
        try {
            // Xóa thông tin phiên đăng nhập
            app.clearUserSession();

            // Chuyển hướng đến màn hình đăng nhập
            chuyenDenDangNhap();

            // Hiển thị thông báo đăng xuất thành công
            messageUtils.showSuccess(R.string.success_logout);
        } catch (Exception e) {
            // Hiển thị thông báo lỗi nếu đăng xuất thất bại
            messageUtils.showError(R.string.error_logout);
        }
    }

    /**
     * Chuyển hướng đến màn hình đăng nhập
     * Xóa tất cả activity trong stack để người dùng không thể quay lại sau khi đăng xuất
     */
    private void chuyenDenDangNhap() {
        Intent intent = new Intent(requireContext(), LogInActivity.class)
                // Thiết lập cờ để xóa tất cả activity trong stack
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }

    @Override
    public void onResume() {
        super.onResume();
        capNhatThongTinNguoiDung();
    }
    /**
     * Được gọi khi view bị hủy
     * Giải phóng tài nguyên binding để tránh rò rỉ bộ nhớ
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}