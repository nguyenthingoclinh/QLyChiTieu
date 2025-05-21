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
    public static final String KEY_NOTIFICATION_TIME = "notification_time";
    public static final String KEY_NOTIFICATIONS_ENABLED = "notifications_enabled";

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
    public static final String DEFAULT_DATE_FORMAT = "dd/MM/yyyy";

    public static final int REQUEST_NOTIFICATION_PERMISSION_CODE = 123;

    // Alert types
    public static final String ALERT_TYPE_DAILY = "DAILY";
    public static final String ALERT_TYPE_WEEKLY = "WEEKLY";
    public static final String ALERT_TYPE_MONTHLY = "MONTHLY";

    // Alert priorities
    public static final int ALERT_PRIORITY_LOW = 1;
    public static final int ALERT_PRIORITY_MEDIUM = 2;
    public static final int ALERT_PRIORITY_HIGH = 3;

    // Request codes
    public static final int REQUEST_EDIT_ALERT = 101;
    public static final int REQUEST_ADD_ALERT = 102;

    // Notification IDs
    public static final String NOTIFICATION_CHANNEL_ID = "spending_alerts_channel";
    public static final String NOTIFICATION_CHANNEL_NAME = "Cảnh báo Chi tiêu";

    // Default values
    public static final String DEFAULT_NOTIFICATION_TIME = "21:00";
}
