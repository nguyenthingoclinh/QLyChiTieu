package com.nhom08.qlychitieu.tien_ich;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Locale;

public class DateTimeUtils {
    private static final Locale VI_LOCALE = new Locale("vi", "VN");

    public static final DateTimeFormatter MONTH_YEAR_FORMATTER =
            DateTimeFormatter.ofPattern("'Thg' M", VI_LOCALE);

    public static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("d 'thg' M EEEE", VI_LOCALE);

    public static final DateTimeFormatter TRANSACTION_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", VI_LOCALE);

    public static long getDayStartTimestamp(long timestamp) {
        return LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(timestamp),
                        ZoneId.systemDefault()
                )
                .withHour(0)
                .withMinute(0)
                .withSecond(0)
                .withNano(0)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();
    }

    public static String formatMonthYear(Calendar calendar) {
        return String.format(VI_LOCALE, "Thg %d",
                calendar.get(Calendar.MONTH) + 1);
    }

    /**
     * Format tháng/năm dạng ngắn gọn (VD: T1/23)
     * @param calendar Đối tượng Calendar chứa thông tin tháng năm cần định dạng
     * @return Chuỗi đại diện cho tháng/năm dạng ngắn gọn (Ví dụ: "T1/23")
     */
    public static String formatMonthYearShort(Calendar calendar) {
        return String.format(VI_LOCALE, "T%d/%02d",
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.YEAR) % 100);
    }
}