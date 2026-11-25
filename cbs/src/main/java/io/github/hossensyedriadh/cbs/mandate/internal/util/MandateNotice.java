package io.github.hossensyedriadh.cbs.mandate.internal.util;

import io.github.hossensyedriadh.cbs.mandate.api.enumerator.MandateKind;
import io.github.hossensyedriadh.cbs.mandate.api.enumerator.MandateRecallReason;
import io.github.hossensyedriadh.cbs.mandate.internal.domain.MandateExecution;
import io.github.hossensyedriadh.cbs.mandate.internal.domain.PaymentMandate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public final class MandateNotice {
    private MandateNotice() {
    }

    public static String reversalDescription(PaymentMandate mandate, MandateExecution execution, MandateRecallReason reason) {
        String prefix = switch (mandate.getKind()) {
            case STANDING_ORDER -> "Standing order recall ";
            case DIRECT_DEBIT -> "Direct debit return ";
            case SWEEP -> "Sweep recall ";
        };
        String amount = execution.getAmount() == null ? "" : " " + execution.getAmount().toPlainString();
        String body = prefix + mandate.getReference() + amount + " " + label(reason);
        return MandateTexts.clip(body, 255);
    }

    public static String label(MandateRecallReason reason) {
        return switch (reason) {
            case PAYER_REQUEST -> "payer request";
            case CREDITOR_ERROR -> "creditor error";
            case DUPLICATE -> "duplicate collection";
            case INCORRECT_AMOUNT -> "incorrect amount";
            case ACCOUNT_CLOSED -> "account closed";
            case NO_AUTHORITY -> "no authority";
        };
    }

    public static String collectionSummary(MandateKind kind, String reference, BigDecimal amount, LocalDate collectionDate) {
        String dated = collectionDate == null ? "undated" : collectionDate.format(DateTimeFormatter.ISO_DATE);
        String value = amount == null ? "0.00" : amount.toPlainString();
        return MandateTexts.prefix(kind) + " " + reference + " of " + value + " on " + dated;
    }
}
