package com.nhom08.qlychitieu.giao_dien.man_hinh_chinh;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.nhom08.qlychitieu.MyApplication;
import com.nhom08.qlychitieu.R;
import com.nhom08.qlychitieu.adapter.TransactionAdapter;
import com.nhom08.qlychitieu.csdl.AppDatabase;
import com.nhom08.qlychitieu.databinding.FragmentHomeBinding;
import com.nhom08.qlychitieu.dialog.MonthYearPickerDialog;
import com.nhom08.qlychitieu.mo_hinh.Category;
import com.nhom08.qlychitieu.mo_hinh.DailyTransaction;
import com.nhom08.qlychitieu.mo_hinh.Transaction;
import com.nhom08.qlychitieu.mo_hinh.Transaction_DateRange;
import com.nhom08.qlychitieu.tien_ich.Constants;
import com.nhom08.qlychitieu.tien_ich.DateTimeUtils;
import com.nhom08.qlychitieu.tien_ich.MessageUtils;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

/**
 * Fragment chính hiển thị màn hình chính của ứng dụng.
 * Hiển thị danh sách giao dịch theo tháng, tổng chi tiêu, thu nhập và số dư.
 */
public class HomeFragment extends Fragment {
    private static final String TAG = "HomeFragment";

    // View binding cho truy cập các thành phần giao diện
    private FragmentHomeBinding binding;

    // Tiện ích hiển thị thông báo
    private MessageUtils messageUtils;

    // Application instance
    private MyApplication myApp;

    // Đối tượng truy cập cơ sở dữ liệu
    private AppDatabase database;

    // Thực thi các tác vụ bất đồng bộ
    private ExecutorService executorService;

    // Danh sách giao dịch hiện tại
    private final List<Transaction> transactionList = new ArrayList<>();

    // Danh sách gốc để khôi phục khi reset tìm kiếm
    private final List<Transaction> originalTransactionList = new ArrayList<>();

    // Adapter hiển thị danh sách giao dịch
    private TransactionAdapter transactionAdapter;

    // Danh sách tất cả các danh mục
    private List<Category> categories = new ArrayList<>();

    // Lịch để theo dõi tháng hiện tại đang hiển thị
    private Calendar currentCalendar;

    /**
     * Tạo và trả về view cho fragment
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        khoiTao();
        thietLapGiaoDien();
        return binding.getRoot();
    }

    /**
     * Khởi tạo các thành phần cần thiết cho fragment
     */
    private void khoiTao() {
        myApp = (MyApplication) requireActivity().getApplication();
        database = myApp.getDatabase();
        executorService = myApp.getExecutorService();
        messageUtils = new MessageUtils(requireContext());
        currentCalendar = Calendar.getInstance();
    }

    /**
     * Thiết lập giao diện và các sự kiện người dùng
     */
    private void thietLapGiaoDien() {
        // Thiết lập RecyclerView
        transactionAdapter = new TransactionAdapter(requireContext());
        binding.recyclerViewTransactions.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerViewTransactions.setAdapter(transactionAdapter);
        transactionAdapter.setOnTransactionDeleteListener(transaction -> {
            executorService.execute(() -> {
                myApp.getDatabase().transactionDao().deleteTransaction(transaction);
                // Room sẽ tự động update LiveData
            });
        });

        // Thiết lập thanh tìm kiếm
        binding.searchView.setOnQueryTextListener(new android.widget.SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                timKiem(query);
                return true;
            }
            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.isEmpty()) khoiPhucDanhSachGoc();
                else timKiem(newText);
                return true;
            }
        });

        // Thiết lập điều hướng tháng
        binding.btnPrevMonth.setOnClickListener(v -> {
            currentCalendar.add(Calendar.MONTH, -1);
            capNhatHienThiThangNam();
            quanSatGiaoDich();
        });
        binding.btnNextMonth.setOnClickListener(v -> {
            if (coTheChuyenDenThangSau()) {
                currentCalendar.add(Calendar.MONTH, 1);
                capNhatHienThiThangNam();
                quanSatGiaoDich();
            }
        });
        binding.btnCalendar.setOnClickListener(v -> hienThiFragmentLich());
        binding.tvMonthYear.setOnClickListener(v -> hienThiDialogChonThang());

        // Thiết lập sự kiện nhấn vào số dư
        View.OnClickListener balanceClickListener = v -> hienThiFragmentTongSoDu();
        binding.tvExpense.setOnClickListener(balanceClickListener);
        binding.tvIncome.setOnClickListener(balanceClickListener);
        binding.tvBalance.setOnClickListener(balanceClickListener);

        capNhatHienThiThangNam();
        quanSatGiaoDich();
    }

    /**
     * Tìm kiếm giao dịch theo từ khoá
     * @param keyword Từ khoá tìm kiếm
     */
    private void timKiem(String keyword) {
        executorService.execute(() -> {
            try {
                int userId = myApp.getCurrentUserId();
                Transaction_DateRange dateRange = Transaction_DateRange.fromCalendar(currentCalendar);

                List<Transaction> searchResults = database.transactionDao()
                        .searchTransactions(userId, keyword)
                        .stream()
                        .filter(t -> dateRange.isInRange(t.getDate()))
                        .collect(Collectors.toList());

                capNhatGiaoDienDanhSachGiaoDich(searchResults);
            } catch (Exception e) {
                Log.e(TAG, "Lỗi tìm kiếm: " + e.getMessage());
                hienThiLoi(R.string.error_search);
            }
        });
    }

    /**
     * Khôi phục về danh sách gốc khi hủy tìm kiếm
     */
    private void khoiPhucDanhSachGoc() {
        capNhatGiaoDienDanhSachGiaoDich(new ArrayList<>(originalTransactionList));
    }

    /**
     * Quan sát và cập nhật giao dịch theo tháng/năm đang chọn
     */
    private void quanSatGiaoDich() {
        executorService.execute(() -> {
            try {
                int userId = myApp.getCurrentUserId();
                Transaction_DateRange dateRange = Transaction_DateRange.fromCalendar(currentCalendar);
                categories = database.categoryDao().getAllCategories(userId);

                LiveData<List<Transaction>> liveTransactions = database.transactionDao()
                        .getTransactionsByDateRangeLive(
                                userId, dateRange.getStartTime(), dateRange.getEndTime()
                        );

                requireActivity().runOnUiThread(() -> {
                    liveTransactions.observe(getViewLifecycleOwner(), this::xuLyGiaoDich);
                });

            } catch (Exception e) {
                Log.e(TAG, "Lỗi quan sát giao dịch: " + e.getMessage());
                hienThiLoi(R.string.error_load_transactions);
            }
        });
    }

    /**
     * Xử lý giao dịch lấy về và cập nhật UI
     * @param transactions Danh sách giao dịch cần xử lý
     */
    private void xuLyGiaoDich(List<Transaction> transactions) {
        if (!isAdded() || getActivity() == null || binding == null) return;

        double totalExpense = 0, totalIncome = 0;
        for (Transaction t : transactions) {
            Category c = timDanhMucTheoId(t.getCategoryId());
            if (c != null) {
                if (Constants.CATEGORY_TYPE_EXPENSE.equals(c.getType()))
                    totalExpense += Math.abs(t.getAmount());
                else
                    totalIncome += t.getAmount();
            }
        }
        capNhatTongThang(totalExpense, totalIncome);

        Map<Long, DailyTransaction> dailyMap = nhomGiaoDichTheoNgay(transactions);

        transactionAdapter.updateData(dailyMap.values(), categories);
        transactionList.clear();
        transactionList.addAll(transactions);
        originalTransactionList.clear();
        originalTransactionList.addAll(transactions);
    }

    /**
     * Cập nhật giao diện danh sách giao dịch khi tìm kiếm/reset
     * @param transactions Danh sách giao dịch cần hiển thị
     */
    private void capNhatGiaoDienDanhSachGiaoDich(List<Transaction> transactions) {
        if (!isAdded() || getActivity() == null || binding == null) return;

        requireActivity().runOnUiThread(() -> {
            Map<Long, DailyTransaction> dailyMap = nhomGiaoDichTheoNgay(transactions);
            transactionAdapter.updateData(dailyMap.values(), categories);
            double totalExpense = 0, totalIncome = 0;
            for (Transaction t : transactions) {
                Category c = timDanhMucTheoId(t.getCategoryId());
                if (c != null) {
                    if (Constants.CATEGORY_TYPE_EXPENSE.equals(c.getType()))
                        totalExpense += Math.abs(t.getAmount());
                    else
                        totalIncome += t.getAmount();
                }
            }
            capNhatTongThang(totalExpense, totalIncome);
        });
    }

    /**
     * Gom nhóm giao dịch theo ngày
     * @param transactions Danh sách giao dịch cần nhóm
     * @return Map với key là timestamp đầu ngày và value là DailyTransaction
     */
    private Map<Long, DailyTransaction> nhomGiaoDichTheoNgay(List<Transaction> transactions) {
        // Sử dụng TreeMap với Comparator đảo ngược để ngày mới nhất lên đầu
        Map<Long, DailyTransaction> dailyMap = new TreeMap<>(Collections.reverseOrder());

        for (Transaction transaction : transactions) {
            long dateKey = DateTimeUtils.getDayStartTimestamp(transaction.getDate());
            DailyTransaction daily = dailyMap.computeIfAbsent(dateKey, DailyTransaction::new);
            daily.addTransaction(transaction, categories);
        }

        // Đảm bảo các transaction trong mỗi ngày cũng được sắp xếp giảm dần theo thời gian
        for (DailyTransaction daily : dailyMap.values()) {
            daily.getTransactions().sort((t1, t2) -> Long.compare(t2.getDate(), t1.getDate()));
        }

        return dailyMap;
    }

    /**
     * Cập nhật tổng chi tiêu, thu nhập và số dư theo tháng
     * @param totalExpense Tổng chi tiêu
     * @param totalIncome Tổng thu nhập
     */
    private void capNhatTongThang(double totalExpense, double totalIncome) {
        if (!isAdded() || getActivity() == null || binding == null) return;
        requireActivity().runOnUiThread(() -> {
            binding.tvExpense.setText(String.format(Locale.getDefault(), "%,d", (long) totalExpense));
            binding.tvIncome.setText(String.format(Locale.getDefault(), "%,d", (long) totalIncome));
            binding.tvBalance.setText(String.format(Locale.getDefault(), "%,d", (long) (totalIncome - totalExpense)));
        });
    }

    /**
     * Hiển thị tháng/năm ở header và cập nhật trạng thái nút tháng trước/sau
     */
    private void capNhatHienThiThangNam() {
        Calendar now = Calendar.getInstance();
        binding.tvMonthYear.setText(DateTimeUtils.formatMonthYear(currentCalendar));
        binding.tvYear.setText(String.valueOf(currentCalendar.get(Calendar.YEAR)));

        // Nút tháng trước
        Calendar prevMonth = (Calendar) currentCalendar.clone();
        prevMonth.add(Calendar.MONTH, -1);
        binding.btnPrevMonth.setText(String.format(Locale.getDefault(), "thg %d %d",
                prevMonth.get(Calendar.MONTH) + 1, prevMonth.get(Calendar.YEAR)));

        // Nút tháng sau
        boolean isCurrentMonth = currentCalendar.get(Calendar.YEAR) == now.get(Calendar.YEAR)
                && currentCalendar.get(Calendar.MONTH) == now.get(Calendar.MONTH);
        if (!isCurrentMonth) {
            Calendar nextMonth = (Calendar) currentCalendar.clone();
            nextMonth.add(Calendar.MONTH, 1);
            binding.btnNextMonth.setText(String.format(Locale.getDefault(), "thg %d %d",
                    nextMonth.get(Calendar.MONTH) + 1, nextMonth.get(Calendar.YEAR)));
            binding.btnNextMonth.setVisibility(View.VISIBLE);
        } else {
            binding.btnNextMonth.setVisibility(View.GONE);
        }
    }

    /**
     * Kiểm tra xem có thể chuyển sang tháng sau không
     * @return true nếu tháng hiện tại không phải là tháng hiện tại của thiết bị
     */
    private boolean coTheChuyenDenThangSau() {
        Calendar now = Calendar.getInstance();
        return currentCalendar.get(Calendar.YEAR) < now.get(Calendar.YEAR) ||
                (currentCalendar.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                        currentCalendar.get(Calendar.MONTH) < now.get(Calendar.MONTH));
    }

    /**
     * Hiển thị dialog chọn tháng/năm
     */
    private void hienThiDialogChonThang() {
        MonthYearPickerDialog dialog = new MonthYearPickerDialog(
                currentCalendar,
                (year, month) -> {
                    currentCalendar.set(Calendar.YEAR, year);
                    currentCalendar.set(Calendar.MONTH, month);
                    currentCalendar.set(Calendar.DAY_OF_MONTH, 1);
                    capNhatHienThiThangNam();
                    quanSatGiaoDich();
                });
        dialog.show(getParentFragmentManager(), "MonthYearPickerDialog");
    }

    /**
     * Chuyển sang fragment lịch
     */
    private void hienThiFragmentLich() {
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.frame_layout, new CalendarFragment())
                .addToBackStack(null)
                .commit();
    }

    /**
     * Chuyển sang fragment tổng số dư
     */
    private void hienThiFragmentTongSoDu() {
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.frame_layout, new TotalBalanceFragment())
                .addToBackStack(null)
                .commit();
    }

    /**
     * Tìm danh mục theo ID
     * @param categoryId ID danh mục cần tìm
     * @return Đối tượng Category hoặc null nếu không tìm thấy
     */
    private Category timDanhMucTheoId(int categoryId) {
        for (Category c : categories) {
            if (c.getCategoryId() == categoryId) return c;
        }
        return null;
    }

    /**
     * Hiển thị thông báo lỗi
     * @param messageId ID của chuỗi thông báo trong resources
     */
    private void hienThiLoi(int messageId) {
        if (!isAdded() || getActivity() == null) return;
        requireActivity().runOnUiThread(() -> messageUtils.showError(messageId));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}