package io.github.hossensyedriadh.cbs.mandate.internal.util;

import io.github.hossensyedriadh.cbs.mandate.api.enumerator.MandateKind;
import io.github.hossensyedriadh.cbs.mandate.internal.domain.PaymentMandate;

public final class MandateTexts {
    private MandateTexts() {
    }

    public static String paymentDescription(PaymentMandate mandate) {
        String prefix = switch (mandate.getKind()) {
            case STANDING_ORDER -> "Standing order ";
            case DIRECT_DEBIT -> "Direct debit ";
            case SWEEP -> "Sweep ";
        };
        String body = prefix + mandate.getReference();
        if (mandate.getNarrative() != null && !mandate.getNarrative().isBlank()) {
            body = body + " " + mandate.getNarrative().trim();
        }
        return clip(body, 255);
    }

    public static String prefix(MandateKind kind) {
        return switch (kind) {
            case STANDING_ORDER -> "Standing order";
            case DIRECT_DEBIT -> "Direct debit";
            case SWEEP -> "Sweep";
        };
    }

    public static String clip(String value, int max) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() <= max) {
            return trimmed;
        }
        return trimmed.substring(0, max);
    }

    public static String text(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
