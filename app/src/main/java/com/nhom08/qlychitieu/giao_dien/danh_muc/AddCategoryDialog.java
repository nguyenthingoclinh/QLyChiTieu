package com.nhom08.qlychitieu.giao_dien.danh_muc;

import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.GridLayoutManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.nhom08.qlychitieu.R;
import com.nhom08.qlychitieu.adapter.IconAdapter;
import com.nhom08.qlychitieu.csdl.AppDatabase;
import com.nhom08.qlychitieu.databinding.DialogAddCategoryBinding;
import com.nhom08.qlychitieu.mo_hinh.Category;
import com.nhom08.qlychitieu.tien_ich.IconProvider;
import com.nhom08.qlychitieu.tien_ich.MessageUtils;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AddCategoryDialog extends DialogFragment {
    private DialogAddCategoryBinding binding;
    private final int userId;
    private final String type;
    private final AppDatabase database;
    private final Runnable onAddSuccess;
    private IconAdapter iconAdapter;
    private MessageUtils messageUtils;
    private String selectedIcon;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public AddCategoryDialog(CategoryActivity categoryActivity, int userId, String type, AppDatabase database, Runnable onAddSuccess) {
        this.userId = userId;
        this.type = type;
        this.database = database;
        this.onAddSuccess = onAddSuccess;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        binding = DialogAddCategoryBinding.inflate(getLayoutInflater());
        messageUtils = new MessageUtils(requireContext());

        setupSpinner();
        setupIconRecyclerView();
        setupButtons();

        return new MaterialAlertDialogBuilder(requireContext())
                .setView(binding.getRoot())
                .create();
    }

    private void setupSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.category_types_display,
                R.layout.item_spinner
        );
        adapter.setDropDownViewResource(R.layout.item_spinner_dropdown);
        binding.spinnerCategoryType.setAdapter(adapter);

        int pos = type.equals("Expense") ? 0 : 1;
        binding.spinnerCategoryType.setSelection(pos);

        binding.spinnerCategoryType.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int pos, long id) {
                loadIcons();
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }

    private void setupIconRecyclerView() {
        iconAdapter = new IconAdapter(requireContext(), icon -> selectedIcon = icon);
        binding.recyclerViewIcons.setLayoutManager(new GridLayoutManager(requireContext(), 4));
        binding.recyclerViewIcons.setAdapter(iconAdapter);
        loadIcons();
    }

    private void loadIcons() {
        int selectedType = binding.spinnerCategoryType.getSelectedItemPosition();
        List<String> icons = (selectedType == 0)
                ? IconProvider.getExpenseIcons(requireContext())
                : IconProvider.getIncomeIcons(requireContext());
        iconAdapter.setIcons(icons);
        if (selectedIcon == null || !icons.contains(selectedIcon)) {
            selectedIcon = icons.get(0);
        }
        iconAdapter.setSelectedIcon(selectedIcon);
    }

    private void setupButtons() {
        binding.btnCancel.setOnClickListener(v -> dismiss());
        binding.btnAdd.setOnClickListener(v -> validateAndAdd());
    }

    private void validateAndAdd() {
        String name = binding.etCategoryName.getText().toString().trim();
        if (TextUtils.isEmpty(name)) {
            binding.etCategoryName.setError(getString(R.string.error_loading_categories));
            return;
        }
        if (selectedIcon == null) {
            messageUtils.showError(R.string.error_no_icon_selected);
            return;
        }
        String type = binding.spinnerCategoryType.getSelectedItemPosition() == 0 ? "Expense" : "Income";
        addCategory(name, type, selectedIcon);
    }

    private void addCategory(String name, String type, String icon) {
        executorService.execute(() -> {
            try {
                Category category = new Category(userId, name, type, icon);
                database.categoryDao().insertCategory(category);
                requireActivity().runOnUiThread(() -> {
                    messageUtils.showSuccess(R.string.category_added_successfully);
                    onAddSuccess.run();
                    dismiss();
                });
            } catch (Exception e) {
                requireActivity().runOnUiThread(() ->
                        messageUtils.showError(R.string.error_adding_category));
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (!executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}