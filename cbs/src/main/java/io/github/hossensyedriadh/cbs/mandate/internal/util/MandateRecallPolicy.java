package io.github.hossensyedriadh.cbs.mandate.internal.util;

import io.github.hossensyedriadh.cbs.mandate.api.enumerator.ExecutionStatus;
import io.github.hossensyedriadh.cbs.mandate.api.enumerator.MandateKind;
import io.github.hossensyedriadh.cbs.mandate.api.enumerator.MandateRecallReason;

import java.time.LocalDate;

public final class MandateRecallPolicy {
    public static final int STANDING_ORDER_BUSINESS_DAYS = 5;
    public static final int DIRECT_DEBIT_BUSINESS_DAYS = 8;

    private MandateRecallPolicy() {
    }

    public static RecallDecision evaluate(MandateKind kind, ExecutionStatus status, Long transactionId, LocalDate collectionDate,
                                          LocalDate businessDate, MandateRecallReason reason, BankHolidayCalendar calendar) {
        if (kind == null || status == null || collectionDate == null || businessDate == null || reason == null) {
            return RecallDecision.deny("Recall is missing mandate, execution, or business-date details");
        }
        if (status == ExecutionStatus.REVERSED) {
            return RecallDecision.deny("This collection has already been recalled");
        }
        if (status != ExecutionStatus.POSTED) {
            return RecallDecision.deny("Only a posted collection can be recalled");
        }
        if (transactionId == null) {
            return RecallDecision.deny("The posted collection has no transaction to reverse");
        }
        if (businessDate.isBefore(collectionDate)) {
            return RecallDecision.deny("A collection cannot be recalled before its collection date");
        }
        if (!reasonAllowed(kind, reason)) {
            return RecallDecision.deny(reason + " is not a valid recall reason for " + kind);
        }

        int elapsed = businessDaysAfter(collectionDate, businessDate, calendar);
        int window = windowFor(kind);
        if (kind == MandateKind.SWEEP && elapsed > 0) {
            return RecallDecision.deny("A sweep can only be recalled on the collection business date");
        }
        if (elapsed > window) {
            return RecallDecision.deny("Recall window of " + window + " business days has passed");
        }
        return RecallDecision.allow(elapsed, window);
    }

    public static boolean reasonAllowed(MandateKind kind, MandateRecallReason reason) {
        return switch (kind) {
            case STANDING_ORDER -> reason == MandateRecallReason.PAYER_REQUEST
                    || reason == MandateRecallReason.DUPLICATE
                    || reason == MandateRecallReason.INCORRECT_AMOUNT;
            case DIRECT_DEBIT -> reason != MandateRecallReason.PAYER_REQUEST;
            case SWEEP -> reason == MandateRecallReason.PAYER_REQUEST || reason == MandateRecallReason.DUPLICATE;
        };
    }

    public static int windowFor(MandateKind kind) {
        return switch (kind) {
            case STANDING_ORDER -> STANDING_ORDER_BUSINESS_DAYS;
            case DIRECT_DEBIT -> DIRECT_DEBIT_BUSINESS_DAYS;
            case SWEEP -> 0;
        };
    }

    public static int businessDaysAfter(LocalDate from, LocalDate to, BankHolidayCalendar calendar) {
        if (from == null || to == null || to.isBefore(from)) {
            return -1;
        }
        int businessDays = 0;
        LocalDate cursor = from;
        while (cursor.isBefore(to)) {
            cursor = cursor.plusDays(1);
            if (calendar.isBusinessDay(cursor)) {
                businessDays++;
            }
        }
        return businessDays;
    }
}
