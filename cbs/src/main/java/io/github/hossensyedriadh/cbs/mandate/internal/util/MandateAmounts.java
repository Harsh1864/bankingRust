package io.github.hossensyedriadh.cbs.mandate.internal.util;

import io.github.hossensyedriadh.cbs.mandate.api.enumerator.AmountType;
import io.github.hossensyedriadh.cbs.mandate.api.enumerator.CapBehaviour;
import io.github.hossensyedriadh.cbs.mandate.api.enumerator.ExecutionSkipReason;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class MandateAmounts {
    public static final int MONEY_SCALE = 2;

    private MandateAmounts() {
    }

    public static AmountDecision decide(AmountType type, BigDecimal configured, BigDecimal available, BigDecimal minimum,
                                        BigDecimal maximum, BigDecimal monthlyPosted, BigDecimal monthlyCap,
                                        CapBehaviour capBehaviour) {
        if (type == null || configured == null || configured.signum() <= 0) {
            return AmountDecision.skip(ExecutionSkipReason.ZERO_AMOUNT);
        }

        CapBehaviour behaviour = capBehaviour == null ? CapBehaviour.SKIP : capBehaviour;
        BigDecimal raw = resolve(type, configured, available);
        if (raw == null || raw.signum() <= 0) {
            return AmountDecision.skip(ExecutionSkipReason.ZERO_AMOUNT);
        }

        if (maximum != null && raw.compareTo(maximum) > 0) {
            if (behaviour == CapBehaviour.CAP) {
                raw = money(maximum);
            } else {
                return AmountDecision.skip(ExecutionSkipReason.ABOVE_MAXIMUM);
            }
        }

        if (monthlyCap != null) {
            BigDecimal already = monthlyPosted == null ? BigDecimal.ZERO : monthlyPosted;
            BigDecimal remaining = money(monthlyCap.subtract(already));
            if (remaining.signum() <= 0) {
                return AmountDecision.skip(ExecutionSkipReason.MONTHLY_CAP);
            }
            if (raw.compareTo(remaining) > 0) {
                if (behaviour == CapBehaviour.CAP) {
                    raw = remaining;
                } else {
                    return AmountDecision.skip(ExecutionSkipReason.MONTHLY_CAP);
                }
            }
        }

        if (minimum != null && raw.compareTo(minimum) < 0) {
            return AmountDecision.skip(ExecutionSkipReason.BELOW_MINIMUM);
        }

        if (raw.signum() <= 0) {
            return AmountDecision.skip(ExecutionSkipReason.ZERO_AMOUNT);
        }

        return AmountDecision.pay(raw);
    }

    public static BigDecimal money(BigDecimal value) {
        return value.setScale(MONEY_SCALE, RoundingMode.HALF_EVEN);
    }

    private static BigDecimal resolve(AmountType type, BigDecimal configured, BigDecimal available) {
        return switch (type) {
            case FIXED -> money(configured);
            case PERCENT_OF_BALANCE -> {
                if (available == null) {
                    yield null;
                }
                yield money(available.multiply(configured).movePointLeft(2));
            }
            case SWEEP_EXCESS -> {
                if (available == null) {
                    yield null;
                }
                yield money(available.subtract(configured));
            }
        };
    }
}
