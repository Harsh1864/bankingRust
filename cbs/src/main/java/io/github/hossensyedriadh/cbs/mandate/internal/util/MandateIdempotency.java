package io.github.hossensyedriadh.cbs.mandate.internal.util;

import java.time.LocalDate;

public final class MandateIdempotency {
    private MandateIdempotency() {
    }

    public static String transferKey(Long mandateId, LocalDate collectionDate) {
        return "md-" + mandateId + "-" + collectionDate;
    }

    public static String withdrawalKey(Long mandateId, LocalDate collectionDate) {
        return "mdw-" + mandateId + "-" + collectionDate;
    }

    public static String feeKey(Long mandateId, LocalDate collectionDate) {
        return "mdfee-" + mandateId + "-" + collectionDate;
    }

    public static String reversalKey(Long executionId) {
        return "mdrev-" + executionId;
    }

    public static String feeReversalKey(Long executionId) {
        return "mdfeerev-" + executionId;
    }
}
