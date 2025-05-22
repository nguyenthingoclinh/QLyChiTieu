package com.nhom08.qlychitieu.tien_ich;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Currency;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class FormatUtils {
    public static final String FORMAT_PREFS = "format_prefs";
    public static final String KEY_CURRENCY_CODE = "currency_code";
    public static final String KEY_NUMBER_FORMAT_STYLE = "number_format_style";
    public static final String KEY_LOCALE_CODE = "locale_code";

    // Các định dạng số phổ biến
    public static final int FORMAT_STYLE_US = 0;       // 1,234.56
    public static final int FORMAT_STYLE_EU = 1;       // 1.234,56
    public static final int FORMAT_STYLE_COMPACT = 2;  // 1K, 1M, 1B

    // Map lưu trữ Locale cho từng vùng
    private static final Map<String, Locale> LOCALE_MAP = new HashMap<>();
    static {
        // Thêm các locale phổ biến
        LOCALE_MAP.put("vi", new Locale("vi", "VN"));
        LOCALE_MAP.put("en", Locale.US);
        LOCALE_MAP.put("fr", Locale.FRANCE);
        LOCALE_MAP.put("de", Locale.GERMANY);
        LOCALE_MAP.put("jp", Locale.JAPAN);
        LOCALE_MAP.put("kr", Locale.KOREA);
        // Thêm các locale khác nếu cần
    }

    // Lưu mã tiền tệ
    public static void saveCurrencyCode(Context context, String currencyCode) {
        SharedPreferences prefs = context.getSharedPreferences(FORMAT_PREFS, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_CURRENCY_CODE, currencyCode).apply();
    }

    // Lấy mã tiền tệ, mặc định là VND
    public static String getCurrencyCode(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(FORMAT_PREFS, Context.MODE_PRIVATE);
        return prefs.getString(KEY_CURRENCY_CODE, "VND");
    }

    // Lưu kiểu định dạng số
    public static void saveNumberFormatStyle(Context context, int formatStyle) {
        SharedPreferences prefs = context.getSharedPreferences(FORMAT_PREFS, Context.MODE_PRIVATE);
        prefs.edit().putInt(KEY_NUMBER_FORMAT_STYLE, formatStyle).apply();
    }

    // Lấy kiểu định dạng số, mặc định là FORMAT_STYLE_US
    public static int getNumberFormatStyle(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(FORMAT_PREFS, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_NUMBER_FORMAT_STYLE, FORMAT_STYLE_US);
    }

    // Lưu mã ngôn ngữ locale
    public static void saveLocaleCode(Context context, String localeCode) {
        SharedPreferences prefs = context.getSharedPreferences(FORMAT_PREFS, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_LOCALE_CODE, localeCode).apply();
    }

    // Lấy mã ngôn ngữ locale, mặc định là "vi"
    public static String getLocaleCode(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(FORMAT_PREFS, Context.MODE_PRIVATE);
        return prefs.getString(KEY_LOCALE_CODE, "vi");
    }

    // Lấy Locale từ mã đã lưu
    public static Locale getLocale(Context context) {
        String localeCode = getLocaleCode(context);
        return LOCALE_MAP.getOrDefault(localeCode, LOCALE_MAP.get("vi"));
    }

    // Lấy các mã tiền tệ phổ biến
    public static String[] getPopularCurrencyCodes() {
        return new String[]{"VND", "USD", "EUR", "GBP", "JPY", "CNY", "KRW", "SGD"};
    }

    // Lấy tên hiển thị của tiền tệ
    public static String getCurrencyDisplayName(String currencyCode) {
        try {
            Currency currency = Currency.getInstance(currencyCode);
            return currency.getDisplayName() + " (" + currency.getSymbol() + ")";
        } catch (Exception e) {
            return currencyCode;
        }
    }

    // Định dạng số tiền
    public static String formatCurrency(Context context, double amount) {
        String currencyCode = getCurrencyCode(context);
        int formatStyle = getNumberFormatStyle(context);

        try {
            Currency currency = Currency.getInstance(currencyCode);

            switch (formatStyle) {
                case FORMAT_STYLE_EU:
                    // European format (1.234,56)
                    String euFormat = String.format("%,.2f", amount).replace(",", "X").replace(".", ",").replace("X", ".");
                    return euFormat + " " + currency.getSymbol();
                case FORMAT_STYLE_COMPACT:
                    // Compact format (1K, 1M)
                    if (amount >= 1_000_000_000) {
                        return String.format("%.1fB %s", amount / 1_000_000_000, currency.getSymbol());
                    } else if (amount >= 1_000_000) {
                        return String.format("%.1fM %s", amount / 1_000_000, currency.getSymbol());
                    } else if (amount >= 1_000) {
                        return String.format("%.1fK %s", amount / 1_000, currency.getSymbol());
                    } else {
                        return String.format("%,.2f %s", amount, currency.getSymbol());
                    }
                case FORMAT_STYLE_US:
                default:
                    // US format (1,234.56)
                    return String.format("%,.2f %s", amount, currency.getSymbol());
            }
        } catch (Exception e) {
            // Fallback formatting if there's any error
            return String.format("%,.2f %s", amount, currencyCode);
        }
    }

    /**
     * Định dạng số tiền theo định dạng tiền tệ Việt Nam
     * @param amount Số tiền cần định dạng
     * @return Chuỗi đã định dạng, ví dụ: "1.000.000"
     */
    public static String formatCurrency(long amount) {
        // Sử dụng NumberFormat để định dạng tiền tệ theo locale Việt Nam
        NumberFormat nf = NumberFormat.getInstance(new Locale("vi", "VN"));
        return nf.format(amount);
    }

    /**
     * Định dạng ngày giờ với định dạng ngày/tháng/năm giờ:phút
     * @param timestamp Thời gian dưới dạng timestamp (milliseconds)
     * @return Chuỗi ngày giờ đã định dạng
     */
    public static String formatDateTime(long timestamp) {
        return Instant.ofEpochMilli(timestamp)
                .atZone(ZoneId.systemDefault())
                .format(DateTimeUtils.TRANSACTION_DATE_FORMATTER);
    }

    /**
     * Định dạng số thực với số chữ số thập phân xác định
     * @param value Giá trị số thực cần định dạng
     * @param decimalPlaces Số chữ số thập phân
     * @return Chuỗi số thực đã định dạng
     */
    public static String formatDouble(double value, int decimalPlaces) {
        return String.format(Locale.getDefault(), "%." + decimalPlaces + "f", value);
    }

    /**
     * Định dạng phần trăm
     * @param value Giá trị cần hiển thị dưới dạng phần trăm (0-1)
     * @param decimalPlaces Số chữ số thập phân
     * @return Chuỗi phần trăm đã định dạng, ví dụ: "75,5%"
     */
    public static String formatPercent(double value, int decimalPlaces) {
        return String.format(Locale.getDefault(), "%." + decimalPlaces + "f%%", value * 100);
    }
}