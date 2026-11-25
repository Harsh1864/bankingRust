package io.github.hossensyedriadh.cbs.mandate.internal.util;

import io.github.hossensyedriadh.cbs.mandate.api.enumerator.ExecutionSkipReason;

import java.math.BigDecimal;

public final class AmountDecision {
    private final BigDecimal amount;
    private final ExecutionSkipReason skipReason;

    private AmountDecision(BigDecimal amount, ExecutionSkipReason skipReason) {
        this.amount = amount;
        this.skipReason = skipReason;
    }

    public static AmountDecision pay(BigDecimal amount) {
        return new AmountDecision(amount, null);
    }

    public static AmountDecision skip(ExecutionSkipReason skipReason) {
        return new AmountDecision(null, skipReason);
    }

    public boolean skipped() {
        return this.skipReason != null;
    }

    public BigDecimal amount() {
        return this.amount;
    }

    public ExecutionSkipReason skipReason() {
        return this.skipReason;
    }
}
