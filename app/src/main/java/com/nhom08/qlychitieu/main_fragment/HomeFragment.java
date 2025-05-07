package com.nhom08.qlychitieu.main_fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.widget.SearchView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.nhom08.qlychitieu.CalendarFragment;
import com.nhom08.qlychitieu.MyApplication;
import com.nhom08.qlychitieu.R;
import com.nhom08.qlychitieu.TotalBalanceFragment;
import com.nhom08.qlychitieu.csdl.AppDatabase;
import com.nhom08.qlychitieu.mo_hinh.Category;
import com.nhom08.qlychitieu.mo_hinh.Transaction;

import java.io.File;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * HomeFragment hiển thị màn hình chính của ứng dụng quản lý chi tiêu.
 * - Hiển thị tổng chi tiêu, thu nhập, số dư của tháng hiện tại.
 * - Cho phép người dùng chuyển tháng, chọn tháng/năm, tìm kiếm giao dịch.
 * - Sử dụng RecyclerView để hiển thị danh sách giao dịch.
 * - Sử dụng LiveData để tự động cập nhật giao dịch.
 */
public class HomeFragment extends Fragment {
    private static final String TAG = "HomeFragment";
    private static final String PREFS_NAME = "MyAppPrefs";
    private static final String EMAIL_KEY = "loggedInEmail";
    private static final String EXPENSE_TYPE = "Expense";
    private final List<DailyTransaction> dailyTransactions = new ArrayList<>();
    private static final DateTimeFormatter MONTH_YEAR_FORMATTER =
            DateTimeFormatter.ofPattern("'Thg' M", new Locale("vi", "VN"));
    private static final DateTimeFormatter PREV_MONTH_FORMATTER =
            DateTimeFormatter.ofPattern("'thg' M yyyy", new Locale("vi", "VN"));

    private AppDatabase database;
    private ExecutorService executorService;
    private Calendar currentCalendar;
    private TextView tvExpense, tvIncome, tvBalance;
    private List<Transaction> transactionList;
    private List<Transaction> originalTransactionList;
    private TransactionAdapter transactionAdapter;
    private List<Category> categories;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        initializeViews(view);
        setupListeners();
        return view;
    }

    private void initializeViews(View view) {
        if (getActivity() != null) {
            database = ((MyApplication) getActivity().getApplication()).getDatabase();
        }
        executorService = Executors.newSingleThreadExecutor();
        currentCalendar = Calendar.getInstance();

        // Initialize views
        tvExpense = view.findViewById(R.id.tvExpense);
        tvIncome = view.findViewById(R.id.tvIncome);
        tvBalance = view.findViewById(R.id.tvBalance);
        TextView tvMonthYear = view.findViewById(R.id.tvMonthYear);
        TextView btnCalendar = view.findViewById(R.id.btnCalendar);
        TextView tvYear = view.findViewById(R.id.tvYear);
        SearchView searchView = view.findViewById(R.id.searchView);
        Button btnPrevMonth = view.findViewById(R.id.btnPrevMonth);
        Button btnNextMonth = view.findViewById(R.id.btnNextMonth);

        // Setup RecyclerView
        RecyclerView recyclerViewTransactions = view.findViewById(R.id.recyclerViewTransactions);
        recyclerViewTransactions.setLayoutManager(new LinearLayoutManager(getContext()));
        transactionList = new ArrayList<>();
        originalTransactionList = new ArrayList<>();
        transactionAdapter = new TransactionAdapter(requireContext());
        recyclerViewTransactions.setAdapter(transactionAdapter);

        // Initial UI updates
        updateMonthYear(tvMonthYear, btnPrevMonth, btnNextMonth, tvYear);
        setupSearchView(searchView);
        setupNavigationButtons(tvMonthYear, btnPrevMonth, btnNextMonth, tvYear);
        setupClickListeners(btnCalendar, tvMonthYear, btnPrevMonth, btnNextMonth);

        // Load initial data
        observeTransactions();
    }

    private void setupSearchView(SearchView searchView) {
        searchView.setVisibility(View.VISIBLE);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchTransactions(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.isEmpty()) {
                    resetToOriginalList();
                } else {
                    searchTransactions(newText);
                }
                return true;
            }
        });
    }

    private void resetToOriginalList() {
        transactionList.clear();
        transactionList.addAll(originalTransactionList);
        updateRecyclerView(new ArrayList<>(originalTransactionList));
    }

    private void setupListeners() {
        View.OnClickListener balanceClickListener = v -> showTotalBalanceFragment();
        tvExpense.setOnClickListener(balanceClickListener);
        tvIncome.setOnClickListener(balanceClickListener);
        tvBalance.setOnClickListener(balanceClickListener);
    }

    // Cập nhật các nút chuyển tháng
    private void setupNavigationButtons(TextView tvMonthYear, Button btnPrevMonth, Button btnNextMonth, TextView tvYear) {
        btnPrevMonth.setOnClickListener(v -> {
            currentCalendar.add(Calendar.MONTH, -1);
            updateMonthYear(tvMonthYear, btnPrevMonth, btnNextMonth, tvYear);
            observeTransactions(); // Cập nhật dữ liệu mới
        });

        btnNextMonth.setOnClickListener(v -> {
            if (canNavigateToNextMonth()) {
                currentCalendar.add(Calendar.MONTH, 1);
                updateMonthYear(tvMonthYear, btnPrevMonth, btnNextMonth, tvYear);
                observeTransactions(); // Cập nhật dữ liệu mới
            }
        });
    }

    private boolean canNavigateToNextMonth() {
        Calendar current = Calendar.getInstance();
        return currentCalendar.get(Calendar.MONTH) < current.get(Calendar.MONTH) ||
                (currentCalendar.get(Calendar.MONTH) == current.get(Calendar.MONTH) &&
                        currentCalendar.get(Calendar.YEAR) <= current.get(Calendar.YEAR));
    }

    private void setupClickListeners(TextView btnCalendar, TextView tvMonthYear,
                                     Button btnPrevMonth, Button btnNextMonth) {
        btnCalendar.setOnClickListener(v -> showCalendarFragment());
        tvMonthYear.setOnClickListener(v ->
                showMonthYearPicker(tvMonthYear, btnPrevMonth, btnNextMonth));
    }

    private void searchTransactions(String keyword) {
        executorService.execute(() -> {
            try {
                int userId = getUserId();
                DateRange dateRange = getMonthDateRange(currentCalendar);

                List<Transaction> searchResults = database.transactionDao()
                        .searchTransactions(userId, keyword);
                List<Transaction> filteredResults = filterTransactionsByDateRange(
                        searchResults, dateRange.startTime, dateRange.endTime);

                requireActivity().runOnUiThread(() -> {
                    transactionList.clear();
                    transactionList.addAll(filteredResults);
                    updateRecyclerView(filteredResults);
                });
            } catch (Exception e) {
                Log.e(TAG, "Error searching transactions: " + e.getMessage(), e);
            }
        });
    }

    private List<Transaction> filterTransactionsByDateRange(List<Transaction> transactions,
                                                            long startTime, long endTime) {
        return transactions.stream()
                .filter(t -> t.getDate() >= startTime && t.getDate() <= endTime)
                .collect(Collectors.toList());
    }

    private static class DateRange {
        final long startTime;
        final long endTime;

        DateRange(long startTime, long endTime) {
            this.startTime = startTime;
            this.endTime = endTime;
        }
    }

    private DateRange getMonthDateRange(Calendar calendar) {
        Calendar startCal = (Calendar) calendar.clone();
        startCal.set(Calendar.DAY_OF_MONTH, 1);
        startCal.set(Calendar.HOUR_OF_DAY, 0);
        startCal.set(Calendar.MINUTE, 0);
        startCal.set(Calendar.SECOND, 0);
        startCal.set(Calendar.MILLISECOND, 0);

        Calendar endCal = (Calendar) calendar.clone();
        endCal.set(Calendar.DAY_OF_MONTH, endCal.getActualMaximum(Calendar.DAY_OF_MONTH));
        endCal.set(Calendar.HOUR_OF_DAY, 23);
        endCal.set(Calendar.MINUTE, 59);
        endCal.set(Calendar.SECOND, 59);
        endCal.set(Calendar.MILLISECOND, 999);

        return new DateRange(startCal.getTimeInMillis(), endCal.getTimeInMillis());
    }

    private int getUserId() {
        SharedPreferences prefs = requireActivity().getSharedPreferences(PREFS_NAME,
                FragmentActivity.MODE_PRIVATE);
        String loggedInEmail = prefs.getString(EMAIL_KEY, null);
        return loggedInEmail != null ?
                database.userDao().getUserByEmail(loggedInEmail).getUserId() : 2;
    }

    private void observeTransactions() {
        executorService.execute(() -> {
            try {
                int userId = getUserId();
                DateRange dateRange = getMonthDateRange(currentCalendar);
                categories = database.categoryDao().getAllCategories(userId);

                // Truy vấn và xử lý trực tiếp
                processTransactions(new ArrayList<>(), userId, dateRange);

            } catch (Exception e) {
                Log.e(TAG, "Error in observeTransactions: " + e.getMessage(), e);
            }
        });
    }

    private void processTransactions(List<Transaction> transactions, int userId, DateRange dateRange) {
        executorService.execute(() -> {
            try {
                // Reset danh sách giao dịch
                dailyTransactions.clear();

                // Uncomment phần LiveData này và sử dụng nó
                LiveData<List<Transaction>> liveTransactions = database.transactionDao()
                        .getTransactionsByDateRangeLive(userId, dateRange.startTime, dateRange.endTime);

                // Observe LiveData trong UI thread
                requireActivity().runOnUiThread(() -> {
                    liveTransactions.observe(getViewLifecycleOwner(), monthTransactions -> {
                        if (monthTransactions != null) {
                            // Tính toán tổng thu chi
                            double totalExpense = 0;
                            double totalIncome = 0;

                            // Tổ chức giao dịch theo ngày và tính tổng
                            Map<Long, DailyTransaction> dailyMap = new TreeMap<>(Collections.reverseOrder());

                            for (Transaction transaction : monthTransactions) {
                                // Tính tổng thu chi
                                Category category = findCategoryById(categories, transaction.getCategoryId());
                                if (category != null) {
                                    if (EXPENSE_TYPE.equals(category.getType())) {
                                        totalExpense += Math.abs(transaction.getAmount());
                                    } else {
                                        totalIncome += transaction.getAmount();
                                    }
                                }

                                // Tổ chức theo ngày
                                long dateKey = getDayStartTimestamp(transaction.getDate());
                                DailyTransaction daily = dailyMap.computeIfAbsent(dateKey, DailyTransaction::new);
                                daily.addTransaction(transaction, categories);
                            }

                            // Cập nhật UI
                            updateMonthlyTotals(totalExpense, totalIncome);
                            transactionAdapter.updateData(dailyMap.values(), categories);
                            updateTransactionLists(monthTransactions);
                        }
                    });
                });

            } catch (Exception e) {
                Log.e(TAG, "Error processing transactions: " + e.getMessage(), e);
            }
        });
    }

    private Map<Long, DailyTransaction> organizeTransactionsByDay(List<Transaction> transactions) {
        Map<Long, DailyTransaction> dailyMap = new TreeMap<>(Collections.reverseOrder());

        for (Transaction transaction : transactions) {
            long dateKey = getDayStartTimestamp(transaction.getDate());
            DailyTransaction daily = dailyMap.computeIfAbsent(dateKey,
                    DailyTransaction::new);
            daily.addTransaction(transaction, categories);
        }

        return dailyMap;
    }

    private long getDayStartTimestamp(long timestamp) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timestamp);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    private void updateUI(List<Transaction> transactions, double totalExpense, double totalIncome,
                          Map<Long, DailyTransaction> dailyTransactions) {
        updateMonthlyTotals(totalExpense, totalIncome);
        transactionAdapter.updateData(dailyTransactions.values(), categories);
        updateTransactionLists(transactions);
    }

    private void updateMonthlyTotals(double totalExpense, double totalIncome) {
        tvExpense.setText(String.format(Locale.getDefault(), "%,d", (long) Math.abs(totalExpense)));
        tvIncome.setText(String.format(Locale.getDefault(), "%,d", (long) totalIncome));
        tvBalance.setText(String.format(Locale.getDefault(), "%,d",
                (long) (totalIncome - Math.abs(totalExpense))));
    }

    private void updateTransactionLists(List<Transaction> transactions) {
        transactionList.clear();
        transactionList.addAll(transactions);
        originalTransactionList.clear();
        originalTransactionList.addAll(transactions);
    }

    public static class DailyTransaction {
        private final long date;
        private double totalExpense;
        private double totalIncome;
        private final List<Transaction> transactions;
        private static final String EXPENSE_TYPE = "Expense";

        public DailyTransaction(long date) {
            this.date = date;
            this.transactions = new ArrayList<>();
        }

        // Getters
        public long getDate() {
            return date;
        }

        public double getTotalExpense() {
            return totalExpense;
        }

        public double getTotalIncome() {
            return totalIncome;
        }

        public List<Transaction> getTransactions() {
            return transactions;
        }

        public void addTransaction(Transaction transaction, List<Category> categories) {
            transactions.add(transaction);
            Category category = findCategoryById(categories, transaction.getCategoryId());
            if (category != null) {
                Log.d(TAG, "Transaction amount: " + transaction.getAmount());
                Log.d(TAG, "Category type: " + category.getType());
                if (EXPENSE_TYPE.equals(category.getType())) {
                    totalExpense += Math.abs(transaction.getAmount());
                } else {
                    totalIncome += transaction.getAmount();
                }
            }
        }

        private static Category findCategoryById(List<Category> categories, int categoryId) {
            if (categories != null) {
                for (Category category : categories) {
                    if (category.getCategoryId() == categoryId) {
                        return category;
                    }
                }
            }
            return null;
        }
    }

    public static class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder> {
        private final Context context;
        private List<DailyTransaction> dailyTransactions = new ArrayList<>();
        private List<Category> categories = new ArrayList<>();
        private static final String EXPENSE_TYPE = "Expense";
        private final DateTimeFormatter DATE_FORMATTER =
                DateTimeFormatter.ofPattern("d 'thg' M EEEE", new Locale("vi", "VN"));
        private final DateTimeFormatter TRANSACTION_DATE_FORMATTER =
                DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", new Locale("vi", "VN"));

        public TransactionAdapter(Context context) {
            this.context = context;
        }

        public void updateData(Collection<DailyTransaction> newDailyTransactions,
                               List<Category> newCategories) {
            dailyTransactions = new ArrayList<>(newDailyTransactions);
            categories = new ArrayList<>(newCategories);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_transaction_home, parent, false);
            return new TransactionViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
            try {
                // Tìm daily transaction và vị trí của transaction trong ngày đó
                int currentPos = 0;
                DailyTransaction currentDaily = null;
                Transaction currentTransaction = null;

                for (DailyTransaction daily : dailyTransactions) {
                    if (position < currentPos + daily.getTransactions().size()) {
                        currentDaily = daily;
                        currentTransaction = daily.getTransactions().get(position - currentPos);
                        break;
                    }
                    currentPos += daily.getTransactions().size();
                }

                if (currentDaily != null && currentTransaction != null) {
                    Category category = findCategoryById(currentTransaction.getCategoryId());

                    // Chỉ hiển thị header cho transaction đầu tiên của mỗi ngày
                    if (position == currentPos) {
                        holder.layoutHeader.setVisibility(View.VISIBLE);
                        bindDailyHeader(holder, currentDaily);
                    } else {
                        holder.layoutHeader.setVisibility(View.GONE);
                    }

                    // Hiển thị chi tiết giao dịch
                    bindTransactionDetails(holder, currentTransaction, category);
                    Transaction finalCurrentTransaction = currentTransaction;
                    holder.itemView.setOnClickListener(v ->
                            showTransactionDetail(finalCurrentTransaction, category));
                }
            } catch (Exception e) {
                Log.e(TAG, "Error in onBindViewHolder: " + e.getMessage(), e);
            }
        }

        private void bindDailyHeader(TransactionViewHolder holder, DailyTransaction daily) {
            String dateText = Instant.ofEpochMilli(daily.date)
                    .atZone(ZoneId.systemDefault())
                    .format(DATE_FORMATTER);
            holder.tvDate.setText(dateText);
            Log.d(TAG, "Daily totalExpense: " + daily.totalExpense);
            Log.d(TAG, "Daily totalIncome: " + daily.totalIncome);
            holder.tvTotalExpense.setText(String.format(Locale.getDefault(), "%,d",
                    (long) daily.totalExpense));
            holder.tvTotalIncome.setText(String.format(Locale.getDefault(), "%,d",
                    (long) daily.totalIncome));
        }

        private void bindTransactionDetails(TransactionViewHolder holder, Transaction transaction,
                                            Category category) {
            if (category != null) {
                setupTransactionView(holder, transaction, category);
            }
        }

        private void setupTransactionView(TransactionViewHolder holder, Transaction transaction,
                                          Category category) {
            boolean isExpense = EXPENSE_TYPE.equals(category.getType());
            int colorRes = isExpense ? R.color.expense_color : R.color.income_color;
            int color = ContextCompat.getColor(context, colorRes);

            holder.ivIcon.setText(category.getIcon());
            holder.ivIcon.setTextColor(color);
            holder.tvDescription.setText(category.getName());

            long amount = Math.abs((long) transaction.getAmount());
            String amountText = String.format(Locale.getDefault(), "%,d", amount);
            holder.tvAmount.setText((isExpense ? "-" : "+") + amountText);
            holder.tvAmount.setTextColor(color);

            setupNote(holder, transaction.getDescription());
        }

        private void setupNote(TransactionViewHolder holder, String description) {
            if (description != null && !description.isEmpty()) {
                holder.tvNote.setVisibility(View.VISIBLE);
                holder.tvNote.setText(description);
            } else {
                holder.tvNote.setVisibility(View.GONE);
            }
        }

        @Override
        public int getItemCount() {
            int total = 0;
            for (DailyTransaction daily : dailyTransactions) {
                total += daily.getTransactions().size();
            }
            return total;
        }

        static class TransactionViewHolder extends RecyclerView.ViewHolder {
            TextView tvDate, tvTotalExpense, tvTotalIncome, tvDescription, tvAmount, ivIcon, tvNote;
            View layoutHeader;

            TransactionViewHolder(@NonNull View itemView) {
                super(itemView);
                tvDate = itemView.findViewById(R.id.tvDate);
                tvTotalExpense = itemView.findViewById(R.id.tvTotalExpense);
                tvTotalIncome = itemView.findViewById(R.id.tvTotalIncome);
                tvDescription = itemView.findViewById(R.id.tvDescription);
                tvAmount = itemView.findViewById(R.id.tvAmount);
                ivIcon = itemView.findViewById(R.id.ivIcon);
                tvNote = itemView.findViewById(R.id.tvNote);
                layoutHeader = itemView.findViewById(R.id.layoutHeader);
            }
        }

        /**
         * Hiển thị chi tiết giao dịch trong dialog
         */
        private void showTransactionDetail(Transaction transaction, Category category) {
            Dialog dialog = new Dialog(context);
            dialog.setContentView(R.layout.dialog_transaction_detail);

            // Thiết lập kích thước dialog
            Window window = dialog.getWindow();
            if (window != null) {
                // Lấy kích thước màn hình
                DisplayMetrics metrics = new DisplayMetrics();
                window.getWindowManager().getDefaultDisplay().getMetrics(metrics);

                // Thiết lập chiều rộng là 90% màn hình
                int width = (int) (metrics.widthPixels * 0.9);
                // Chiều cao tự động (WRAP_CONTENT)
                int height = WindowManager.LayoutParams.WRAP_CONTENT;

                // Áp dụng kích thước mới
                window.setLayout(width, height);

                // Thêm background với corner radius (nếu muốn)
                window.setBackgroundDrawable(new ColorDrawable(Color.WHITE));
            }

            // Ánh xạ views
            TextView tvDialogTitle = dialog.findViewById(R.id.tvDialogTitle);
            TextView tvDialogCategory = dialog.findViewById(R.id.tvDialogCategory);
            TextView tvDialogAmount = dialog.findViewById(R.id.tvDialogAmount);
            TextView tvDialogDate = dialog.findViewById(R.id.tvDialogDate);
            TextView tvDialogNote = dialog.findViewById(R.id.tvDialogNote);
            ImageView ivDialogImage = dialog.findViewById(R.id.ivDialogImage);
            Button btnClose = dialog.findViewById(R.id.btnClose);

            // Set tiêu đề dựa vào loại giao dịch
            boolean isExpense = category != null && EXPENSE_TYPE.equals(category.getType());
            tvDialogTitle.setText(isExpense ? "Chi tiết khoản chi" : "Chi tiết khoản thu");

            // Set thông tin category
            tvDialogCategory.setText(category != null ? category.getName() : "Unknown");

            // Set số tiền với định dạng và màu sắc
            long amount = Math.abs((long) transaction.getAmount());
            String amountText = String.format(Locale.getDefault(), "%,d", amount);
            tvDialogAmount.setText((isExpense ? "-" : "+") + amountText);
            tvDialogAmount.setTextColor(ContextCompat.getColor(context,
                    isExpense ? R.color.expense_color : R.color.income_color));

            // Set ngày giờ
            String dateText = Instant.ofEpochMilli(transaction.getDate())
                    .atZone(ZoneId.systemDefault())
                    .format(TRANSACTION_DATE_FORMATTER);
            tvDialogDate.setText(dateText);

            // Set ghi chú
            String note = transaction.getDescription();
            tvDialogNote.setText(note != null && !note.isEmpty() ? note : "Không có ghi chú");

            // Hiển thị hình ảnh nếu có
            setupDialogImage(transaction.getImagePath(), ivDialogImage);
            if (transaction.getImagePath() != null && !transaction.getImagePath().isEmpty()) {
                ivDialogImage.setVisibility(View.VISIBLE);

                // Load ảnh bằng Bitmap
                Bitmap bitmap = BitmapFactory.decodeFile(transaction.getImagePath());
                if (bitmap != null) {
                    ivDialogImage.setImageBitmap(bitmap);

                    // Thêm sự kiện click để xem ảnh full màn hình
                    ivDialogImage.setOnClickListener(v -> {
                        Dialog fullImageDialog = new Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
                        ImageView fullImage = new ImageView(context);
                        fullImage.setLayoutParams(new ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT));
                        fullImage.setScaleType(ImageView.ScaleType.FIT_CENTER);
                        fullImageDialog.setContentView(fullImage);

                        fullImage.setImageBitmap(bitmap);
                        fullImage.setOnClickListener(view -> fullImageDialog.dismiss());
                        fullImageDialog.show();
                    });
                }
            } else {
                ivDialogImage.setVisibility(View.GONE);
            }

            btnClose.setOnClickListener(v -> dialog.dismiss());
            dialog.show();
        }

        /**
         * Thiết lập hình ảnh cho dialog chi tiết giao dịch
         */
        private void setupDialogImage(String imagePath, ImageView imageView) {
            if (imagePath != null && !imagePath.isEmpty()) {
                File imgFile = new File(imagePath);
                if (imgFile.exists()) {
                    try {
                        Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                        if (myBitmap != null) {
                            imageView.setImageBitmap(myBitmap);
                            imageView.setVisibility(View.VISIBLE);
                            return;
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error loading image: " + e.getMessage(), e);
                    }
                }
            }
            imageView.setVisibility(View.GONE);
        }

        /**
         * Tìm Category theo ID
         */
        private Category findCategoryById(int categoryId) {
            if (categories != null) {
                return categories.stream()
                        .filter(c -> c.getCategoryId() == categoryId)
                        .findFirst()
                        .orElse(null);
            }
            return null;
        }
    }

    /**
     * Hiển thị fragment tổng số dư
     */
    private void showTotalBalanceFragment() {
        TotalBalanceFragment fragment = new TotalBalanceFragment();
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.frame_layout, fragment)
                .addToBackStack(null)
                .commit();
    }

    /**
     * Hiển thị fragment lịch
     */
    private void showCalendarFragment() {
        CalendarFragment fragment = new CalendarFragment();
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.frame_layout, fragment)
                .addToBackStack(null)
                .commit();
    }

    /**
     * Cập nhật hiển thị tháng năm và trạng thái của các nút điều hướng
     */
    private void updateMonthYear(TextView tvMonthYear, Button btnPrevMonth, Button btnNextMonth, TextView tvYear) {
        Calendar now = Calendar.getInstance();

        // Format tháng năm hiện tại
        String monthYear = String.format(Locale.getDefault(), "Thg %d",
                currentCalendar.get(Calendar.MONTH) + 1);
        tvMonthYear.setText(monthYear);

        // Kiểm tra xem có phải tháng hiện tại không
        boolean isCurrentMonth = currentCalendar.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                currentCalendar.get(Calendar.MONTH) == now.get(Calendar.MONTH);

        // Format và set text cho nút Previous
        Calendar prevMonth = (Calendar) currentCalendar.clone();
        prevMonth.add(Calendar.MONTH, -1);
        String prevMonthText = String.format(Locale.getDefault(), "thg %d %d",
                prevMonth.get(Calendar.MONTH) + 1,
                prevMonth.get(Calendar.YEAR));
        btnPrevMonth.setText(prevMonthText);

        // Hiển thị/ẩn và set text cho nút Next
        if (!isCurrentMonth) {
            Calendar nextMonth = (Calendar) currentCalendar.clone();
            nextMonth.add(Calendar.MONTH, 1);
            String nextMonthText = String.format(Locale.getDefault(), "thg %d %d",
                    nextMonth.get(Calendar.MONTH) + 1,
                    nextMonth.get(Calendar.YEAR));
            btnNextMonth.setText(nextMonthText);
            btnNextMonth.setVisibility(View.VISIBLE);
        } else {
            btnNextMonth.setVisibility(View.GONE);
        }

        // Cập nhật năm ở header nếu có TextView year

        if (tvYear != null) {
            tvYear.setText(String.valueOf(currentCalendar.get(Calendar.YEAR)));
        }
    }

    /**
     * Hiển thị dialog chọn tháng năm
     */
    private void showMonthYearPicker(TextView tvMonthYear, Button btnPrevMonth, Button btnNextMonth) {
        MonthYearPickerDialog dialog = new MonthYearPickerDialog(
                currentCalendar,
                (year, month) -> {
                    currentCalendar.set(Calendar.YEAR, year);
                    currentCalendar.set(Calendar.MONTH, month);
                    currentCalendar.set(Calendar.DAY_OF_MONTH, 1); // Reset về ngày đầu tháng
                    updateMonthYear(tvMonthYear, btnPrevMonth, btnNextMonth, null);
                    observeTransactions(); // Cập nhật dữ liệu
                });
        dialog.show(getParentFragmentManager(), "MonthYearPickerDialog");
    }

    /**
     * Cập nhật RecyclerView với danh sách giao dịch mới
     */
    private void updateRecyclerView(List<Transaction> newList) {
        try {
            transactionList.clear();
            transactionList.addAll(newList);
            transactionAdapter.notifyDataSetChanged();
        } catch (Exception e) {
            Log.e(TAG, "Error in updateRecyclerView: " + e.getMessage(), e);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }

    public static class MonthYearPickerDialog extends DialogFragment {
        private static final String[] MONTHS = {"Tháng 1", "Tháng 2", "Tháng 3", "Tháng 4",
                "Tháng 5", "Tháng 6", "Tháng 7", "Tháng 8", "Tháng 9", "Tháng 10", "Tháng 11", "Tháng 12"};

        private NumberPicker monthPicker;
        private NumberPicker yearPicker;
        private final OnDateSetListener listener;
        private final Calendar calendar;

        public interface OnDateSetListener {
            void onDateSet(int year, int month);
        }

        public MonthYearPickerDialog(Calendar calendar, OnDateSetListener listener) {
            this.calendar = calendar;
            this.listener = listener;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
            LayoutInflater inflater = requireActivity().getLayoutInflater();

            View dialog = inflater.inflate(R.layout.dialog_month_year_picker, null);
            monthPicker = dialog.findViewById(R.id.monthPicker);
            yearPicker = dialog.findViewById(R.id.yearPicker);

            // Cấu hình NumberPicker cho tháng
            monthPicker.setMinValue(0);
            monthPicker.setMaxValue(11);
            monthPicker.setDisplayedValues(MONTHS);
            monthPicker.setValue(calendar.get(Calendar.MONTH));

            // Cấu hình NumberPicker cho năm
            Calendar now = Calendar.getInstance();
            yearPicker.setMinValue(2020); // Năm bắt đầu
            yearPicker.setMaxValue(now.get(Calendar.YEAR));
            yearPicker.setValue(calendar.get(Calendar.YEAR));

            builder.setView(dialog)
                    .setPositiveButton("OK", (dialog1, id) -> {
                        listener.onDateSet(yearPicker.getValue(), monthPicker.getValue());
                    })
                    .setNegativeButton("Hủy", (dialog1, id) -> {
                        if (dialog1 != null) {
                            dialog1.cancel();
                        }
                    });

            return builder.create();
        }
    }
    private Category findCategoryById(List<Category> categories, int categoryId) {
        if (categories != null) {
            for (Category category : categories) {
                if (category.getCategoryId() == categoryId) {
                    return category;
                }
            }
        }
        return null;
    }
}