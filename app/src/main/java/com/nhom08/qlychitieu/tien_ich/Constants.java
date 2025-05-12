package com.nhom08.qlychitieu.tien_ich;

public class Constants {
    // Image related
    public static final String RECEIPTS_DIR = "receipt_images";
    public static final int MAX_IMAGE_DIMENSION = 1024;
    public static final int IMAGE_QUALITY = 80;

    // Preferences
    public static final String PREFS_NAME = "app_preferences";

    // User Session Keys
    public static final String KEY_USER_ID = "user_id";
    public static final String KEY_USER_EMAIL = "user_email";
    public static final String KEY_LOGIN_TYPE = "login_type";
    public static final String KEY_IS_LOGGED_IN = "is_logged_in";

    // Login Types
    public static final String LOGIN_TYPE_NORMAL = "normal";
    public static final String LOGIN_TYPE_GOOGLE = "google";

    // Category types
    public static final String CATEGORY_TYPE_EXPENSE = "Expense";
    public static final String CATEGORY_TYPE_INCOME = "Income";

    // Validation
    public static final int MIN_PASSWORD_LENGTH = 8;
    public static final String PASSWORD_PATTERN = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=\\S+$).{8,}$";
    public static final String EMAIL_PATTERN = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";

    // Default Values
    public static final String DEFAULT_CURRENCY = "VND";
    public static final String DEFAULT_DATE_FORMAT = "dd/MM/yyyy";
    public static final String DEFAULT_TIME_FORMAT = "HH:mm";


}
