package com.nhom08.qlychitieu.giao_dien.danh_muc;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.GridLayoutManager;

import com.nhom08.qlychitieu.R;
import com.nhom08.qlychitieu.adapter.IconAdapter;
import com.nhom08.qlychitieu.csdl.AppDatabase;
import com.nhom08.qlychitieu.databinding.DialogAddCategoryBinding;
import com.nhom08.qlychitieu.mo_hinh.Category;
import com.nhom08.qlychitieu.tien_ich.MessageUtils;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AddCategoryDialog extends DialogFragment {
    private static final String TAG = AddCategoryDialog.class.getSimpleName();

    private DialogAddCategoryBinding binding;
    private IconAdapter iconAdapter;
    private final MessageUtils messageUtils;
    private final int userId;
    private final AppDatabase database;
    private final Runnable onCategoryAdded;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private String selectedIcon = "";
    private String[] displayValues;
    private String[] dbValues;

    public AddCategoryDialog(Context context, int userId,
                             AppDatabase database, Runnable onCategoryAdded) {
        this.userId = userId;
        this.database = database;
        this.onCategoryAdded = onCategoryAdded;
        this.messageUtils = new MessageUtils(context);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = DialogAddCategoryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupViews();
        setupSpinner();
        setupIconRecyclerView();
    }

    private void setupViews() {
        binding.btnCancel.setOnClickListener(v -> dismiss());
        binding.btnAdd.setOnClickListener(v -> saveCategory());
    }

    private void setupSpinner() {
        // Lấy mảng giá trị hiển thị và giá trị DB
        displayValues = getResources().getStringArray(R.array.category_types_display);
        dbValues = getResources().getStringArray(R.array.category_types_values);

        // Tạo adapter với layout tùy chỉnh
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                requireContext(),
                R.layout.item_spinner,
                displayValues
        ) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView textView = (TextView) view;
                textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.black));
                return view;
            }

            @Override
            public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView textView = (TextView) view;
                textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.black));
                textView.setPadding(16, 16, 16, 16);
                return view;
            }
        };

        adapter.setDropDownViewResource(R.layout.item_spinner_dropdown);
        binding.spinnerCategoryType.setAdapter(adapter);
    }

    private void setupIconRecyclerView() {
        iconAdapter = new IconAdapter(getContext(), this::onIconSelected);
        binding.recyclerViewIcons.setLayoutManager(
                new GridLayoutManager(getContext(), 4));
        binding.recyclerViewIcons.setAdapter(iconAdapter);
        loadIcons();
    }

    private void loadIcons() {
        List<String> icons = Arrays.asList(
                getString(R.string.icon_shopping_cart),
                getString(R.string.icon_restaurant),
                getString(R.string.icon_phone),
                getString(R.string.icon_games),
                getString(R.string.icon_health_and_safety),
                getString(R.string.icon_school),
                getString(R.string.icon_sports),
                getString(R.string.icon_people),
                getString(R.string.icon_directions_car),
                getString(R.string.icon_laundry),
                getString(R.string.icon_car_repair),
                getString(R.string.icon_credit_card),
                getString(R.string.icon_card_giftcard),
                getString(R.string.icon_account_balance),
                getString(R.string.icon_confirmation_number),
                getString(R.string.icon_child_friendly),
                getString(R.string.icon_local_florist),
                getString(R.string.icon_local_grocery_store),
                getString(R.string.icon_spa),
                getString(R.string.icon_trending_up),
                getString(R.string.icon_sports_volleyball),
                getString(R.string.icon_movie),
                getString(R.string.icon_rocket_launch),
                getString(R.string.icon_videogame_asset),
                getString(R.string.icon_toys),
                getString(R.string.icon_sports_esports),
                getString(R.string.icon_headphones),
                getString(R.string.icon_palette),
                getString(R.string.icon_camera_alt),
                getString(R.string.icon_music_note),
                getString(R.string.icon_sports_soccer),
                getString(R.string.icon_bread_slice),
                getString(R.string.icon_fastfood),
                getString(R.string.icon_cake),
                getString(R.string.icon_local_dining),
                getString(R.string.icon_icecream),
                getString(R.string.icon_local_pizza),
                getString(R.string.icon_lunch_dining),
                getString(R.string.icon_kebab_dining),
                getString(R.string.icon_bakery_dining),
                getString(R.string.icon_ramen_dining),
                getString(R.string.icon_local_drink),
                getString(R.string.icon_coffee)
        );
        iconAdapter.setIcons(icons);
    }

    private void onIconSelected(String icon) {
        this.selectedIcon = icon;
    }

    private void saveCategory() {
        String name = binding.etCategoryName.getText().toString().trim();
        // Lấy vị trí được chọn từ spinner
        int selectedPosition = binding.spinnerCategoryType.getSelectedItemPosition();
        // Lấy giá trị DB tương ứng
        String type = dbValues[selectedPosition];

        if (name.isEmpty()) {
            messageUtils.showError(R.string.enter_category_name);
            return;
        }

        if (selectedIcon.isEmpty()) {
            messageUtils.showError(R.string.select_category_icon);
            return;
        }

        Category category = new Category();
        category.setName(name);
        category.setIcon(selectedIcon);
        category.setType(type);
        category.setUserId(userId);

        executorService.execute(() -> {
            try {
                database.categoryDao().insertCategory(category);
                requireActivity().runOnUiThread(() -> {
                    messageUtils.showSuccess(R.string.category_added_successfully);
                    onCategoryAdded.run();
                    dismiss();
                });
            } catch (Exception e) {
                Log.e(TAG, "Error adding category: " + e.getMessage());
                requireActivity().runOnUiThread(() ->
                        messageUtils.showError(R.string.error_adding_category));
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        if (!executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}