package io.github.hossensyedriadh.cbs.mandate.internal.util;

public record RecallDecision(boolean allowed, String detail, int businessDaysElapsed, int windowBusinessDays) {
    public static RecallDecision allow(int businessDaysElapsed, int windowBusinessDays) {
        return new RecallDecision(true, null, businessDaysElapsed, windowBusinessDays);
    }

    public static RecallDecision deny(String detail) {
        return new RecallDecision(false, detail, -1, -1);
    }
}
