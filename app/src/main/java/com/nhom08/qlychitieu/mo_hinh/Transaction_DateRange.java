package com.nhom08.qlychitieu.mo_hinh;
import java.util.Calendar;

public class Transaction_DateRange {
    private final long startTime;
    private final long endTime;

    public Transaction_DateRange(long startTime, long endTime) {
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public static Transaction_DateRange fromCalendar(Calendar calendar) {
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

        return new Transaction_DateRange(startCal.getTimeInMillis(), endCal.getTimeInMillis());
    }

    public long getStartTime() {
        return startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public boolean isInRange(long timestamp) {
        return timestamp >= startTime && timestamp <= endTime;
    }
}
