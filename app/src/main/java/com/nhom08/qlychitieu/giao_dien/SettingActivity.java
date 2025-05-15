package com.nhom08.qlychitieu.giao_dien;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import androidx.appcompat.app.AppCompatActivity;

import com.nhom08.qlychitieu.R;
import com.nhom08.qlychitieu.databinding.ActivitySettingBinding;
import com.nhom08.qlychitieu.tien_ich.FormatUtils;
import com.nhom08.qlychitieu.tien_ich.ThemeUtils;

import java.util.ArrayList;
import java.util.List;

public class SettingActivity extends AppCompatActivity {

    private ActivitySettingBinding binding;
    private String[] currencyCodes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Back button
        binding.tvBack.setOnClickListener(v -> finish());

        // Setup all controls
        setupThemeRadioGroup();
        setupCurrencySpinner();
        setupNumberFormatRadioGroup();

        // Update preview
        updatePreview();
    }

    private void setupThemeRadioGroup() {
        // Get current theme setting
        int currentTheme = ThemeUtils.getThemeMode(this);

        // Set the appropriate radio button
        switch (currentTheme) {
            case ThemeUtils.THEME_LIGHT:
                binding.radioGroupTheme.check(R.id.radioLightTheme);
                break;
            case ThemeUtils.THEME_DARK:
                binding.radioGroupTheme.check(R.id.radioDarkTheme);
                break;
            case ThemeUtils.THEME_FOLLOW_SYSTEM:
            default:
                binding.radioGroupTheme.check(R.id.radioSystemDefault);
                break;
        }

        // Set listener for theme changes
        binding.radioGroupTheme.setOnCheckedChangeListener((group, checkedId) -> {
            int themeMode;

            if (checkedId == R.id.radioLightTheme) {
                themeMode = ThemeUtils.THEME_LIGHT;
            } else if (checkedId == R.id.radioDarkTheme) {
                themeMode = ThemeUtils.THEME_DARK;
            } else {
                themeMode = ThemeUtils.THEME_FOLLOW_SYSTEM;
            }

            // Save and apply the theme
            ThemeUtils.saveThemeMode(this, themeMode);
        });
    }

    private void setupCurrencySpinner() {
        // Get currency codes
        currencyCodes = FormatUtils.getPopularCurrencyCodes();
        List<String> currencyNames = new ArrayList<>();

        // Create display names for currencies
        for (String code : currencyCodes) {
            currencyNames.add(FormatUtils.getCurrencyDisplayName(code));
        }

        // Setup spinner adapter
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                currencyNames
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerCurrency.setAdapter(adapter);

        // Set current selection
        String currentCurrency = FormatUtils.getCurrencyCode(this);
        for (int i = 0; i < currencyCodes.length; i++) {
            if (currencyCodes[i].equals(currentCurrency)) {
                binding.spinnerCurrency.setSelection(i);
                break;
            }
        }

        // Set listener
        binding.spinnerCurrency.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                FormatUtils.saveCurrencyCode(SettingActivity.this, currencyCodes[position]);
                updatePreview();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupNumberFormatRadioGroup() {
        // Get current format
        int currentFormat = FormatUtils.getNumberFormatStyle(this);

        // Set the appropriate radio button
        switch (currentFormat) {
            case FormatUtils.FORMAT_STYLE_EU:
                binding.radioGroupNumberFormat.check(R.id.radioFormatEU);
                break;
            case FormatUtils.FORMAT_STYLE_COMPACT:
                binding.radioGroupNumberFormat.check(R.id.radioFormatCompact);
                break;
            case FormatUtils.FORMAT_STYLE_US:
            default:
                binding.radioGroupNumberFormat.check(R.id.radioFormatUS);
                break;
        }

        // Set listener
        binding.radioGroupNumberFormat.setOnCheckedChangeListener((group, checkedId) -> {
            int formatStyle;

            if (checkedId == R.id.radioFormatEU) {
                formatStyle = FormatUtils.FORMAT_STYLE_EU;
            } else if (checkedId == R.id.radioFormatCompact) {
                formatStyle = FormatUtils.FORMAT_STYLE_COMPACT;
            } else {
                formatStyle = FormatUtils.FORMAT_STYLE_US;
            }

            FormatUtils.saveNumberFormatStyle(this, formatStyle);
            updatePreview();
        });
    }

    private void updatePreview() {
        // Update preview with different amounts
        binding.textPreviewSmall.setText(FormatUtils.formatCurrency(this, 1234.56));
        binding.textPreviewMedium.setText(FormatUtils.formatCurrency(this, 123456.78));
        binding.textPreviewLarge.setText(FormatUtils.formatCurrency(this, 1234567.89));
    }
}