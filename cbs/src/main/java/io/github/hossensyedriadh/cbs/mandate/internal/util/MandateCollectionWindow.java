package io.github.hossensyedriadh.cbs.mandate.internal.util;

import java.time.LocalDate;

public final class MandateCollectionWindow {
    public static final int MAX_LAG_DAYS = 7;

    private MandateCollectionWindow() {
    }

    public static boolean shouldRun(LocalDate businessDate, LocalDate systemDate) {
        if (businessDate == null || systemDate == null) {
            return false;
        }
        if (businessDate.isAfter(systemDate)) {
            return false;
        }
        return !businessDate.isBefore(systemDate.minusDays(MAX_LAG_DAYS));
    }

    public static String holdReason(LocalDate businessDate, LocalDate systemDate) {
        if (businessDate == null || systemDate == null) {
            return "Business date is not available";
        }
        if (businessDate.isAfter(systemDate)) {
            return "Business date is ahead of the system date";
        }
        if (businessDate.isBefore(systemDate.minusDays(MAX_LAG_DAYS))) {
            return "Business date is more than " + MAX_LAG_DAYS + " days behind the system date";
        }
        return null;
    }
}
