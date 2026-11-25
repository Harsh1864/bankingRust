package io.github.hossensyedriadh.cbs.mandate.api.enumerator;

public enum ExecutionSkipReason {
    ZERO_AMOUNT,
    BELOW_MINIMUM,
    ABOVE_MAXIMUM,
    MONTHLY_CAP,
    INSUFFICIENT_FUNDS,
    ACCOUNT_NOT_ACTIVE,
    CREDITOR_UNAVAILABLE,
    CURRENCY_MISMATCH,
    HOLIDAY,
    UNEXPECTED
}
