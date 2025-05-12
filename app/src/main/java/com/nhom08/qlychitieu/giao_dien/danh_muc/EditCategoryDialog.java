package com.nhom08.qlychitieu.giao_dien.danh_muc;

import android.app.Dialog;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.GridLayoutManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.nhom08.qlychitieu.R;
import com.nhom08.qlychitieu.adapter.IconAdapter;
import com.nhom08.qlychitieu.csdl.AppDatabase;
import com.nhom08.qlychitieu.databinding.DialogEditCategoryBinding;
import com.nhom08.qlychitieu.mo_hinh.Category;
import com.nhom08.qlychitieu.tien_ich.IconProvider;
import com.nhom08.qlychitieu.tien_ich.MessageUtils;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EditCategoryDialog extends DialogFragment {
    private DialogEditCategoryBinding binding;
    private final Category category;
    private final AppDatabase database;
    private final Runnable onCategoryEdited;
    private IconAdapter iconAdapter;
    private MessageUtils messageUtils;
    private String selectedIcon;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public EditCategoryDialog(CategoryActivity categoryActivity, @NonNull Category category,
                              @NonNull AppDatabase database,
                              @NonNull Runnable onCategoryEdited) {
        this.category = category;
        this.database = database;
        this.onCategoryEdited = onCategoryEdited;
        this.selectedIcon = category.getIcon();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        binding = DialogEditCategoryBinding.inflate(getLayoutInflater());
        messageUtils = new MessageUtils(requireContext());

        setupSpinner();
        setupIconRecyclerView();
        loadCurrentCategory();
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

        int pos = category.getType().equals("Expense") ? 0 : 1;
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
        iconAdapter = new IconAdapter(requireContext(), this::onIconSelected);
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

    private void loadCurrentCategory() {
        binding.etCategoryName.setText(category.getName());
    }

    private void onIconSelected(String icon) {
        selectedIcon = icon;
    }

    private void setupButtons() {
        binding.btnCancel.setOnClickListener(v -> dismiss());
        binding.btnSave.setOnClickListener(v -> saveCategory());
    }

    private void saveCategory() {
        String name = binding.etCategoryName.getText().toString().trim();
        if (name.isEmpty()) {
            binding.etCategoryName.setError(getString(R.string.error_loading_categories));
            return;
        }
        String type = binding.spinnerCategoryType.getSelectedItemPosition() == 0 ? "Expense" : "Income";
        category.setName(name);
        category.setType(type);
        category.setIcon(selectedIcon);

        executorService.execute(() -> {
            try {
                database.categoryDao().updateCategory(category);
                requireActivity().runOnUiThread(() -> {
                    messageUtils.showSuccess(R.string.category_updated_successfully);
                    onCategoryEdited.run();
                    dismiss();
                });
            } catch (Exception e) {
                requireActivity().runOnUiThread(() ->
                        messageUtils.showError(R.string.error_updating_category));
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