package io.github.hossensyedriadh.cbs.mandate.internal.util;

import io.github.hossensyedriadh.cbs.mandate.api.enumerator.AmountType;
import io.github.hossensyedriadh.cbs.mandate.api.enumerator.CapBehaviour;
import io.github.hossensyedriadh.cbs.mandate.api.enumerator.ExecutionSkipReason;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MandateAmountsTest {
    @Test
    void fixedAmountIsTakenAsConfigured() {
        AmountDecision decision = MandateAmounts.decide(AmountType.FIXED, new BigDecimal("25.50"), null,
                null, null, null, null, CapBehaviour.SKIP);
        assertFalse(decision.skipped());
        assertEquals(new BigDecimal("25.50"), decision.amount());
    }

    @Test
    void percentOfBalanceUsesTheAvailableBalance() {
        AmountDecision decision = MandateAmounts.decide(AmountType.PERCENT_OF_BALANCE, new BigDecimal("12.50"),
                new BigDecimal("200.00"), null, null, null, null, CapBehaviour.SKIP);
        assertEquals(new BigDecimal("25.00"), decision.amount());
    }

    @Test
    void sweepPaysOnlyTheExcessOverTheThreshold() {
        AmountDecision excess = MandateAmounts.decide(AmountType.SWEEP_EXCESS, new BigDecimal("200.00"),
                new BigDecimal("250.00"), null, null, null, null, CapBehaviour.SKIP);
        assertEquals(new BigDecimal("50.00"), excess.amount());

        AmountDecision nothing = MandateAmounts.decide(AmountType.SWEEP_EXCESS, new BigDecimal("200.00"),
                new BigDecimal("150.00"), null, null, null, null, CapBehaviour.SKIP);
        assertTrue(nothing.skipped());
        assertEquals(ExecutionSkipReason.ZERO_AMOUNT, nothing.skipReason());
    }

    @Test
    void maximumCanCapOrSkip() {
        AmountDecision capped = MandateAmounts.decide(AmountType.FIXED, new BigDecimal("50.00"), null,
                null, new BigDecimal("40.00"), null, null, CapBehaviour.CAP);
        assertEquals(new BigDecimal("40.00"), capped.amount());

        AmountDecision skipped = MandateAmounts.decide(AmountType.FIXED, new BigDecimal("50.00"), null,
                null, new BigDecimal("40.00"), null, null, CapBehaviour.SKIP);
        assertEquals(ExecutionSkipReason.ABOVE_MAXIMUM, skipped.skipReason());
    }

    @Test
    void monthlyCapLeavesOnlyTheRemainingRoom() {
        AmountDecision capped = MandateAmounts.decide(AmountType.FIXED, new BigDecimal("30.00"), null,
                null, null, new BigDecimal("80.00"), new BigDecimal("100.00"), CapBehaviour.CAP);
        assertEquals(new BigDecimal("20.00"), capped.amount());

        AmountDecision skipped = MandateAmounts.decide(AmountType.FIXED, new BigDecimal("30.00"), null,
                null, null, new BigDecimal("80.00"), new BigDecimal("100.00"), CapBehaviour.SKIP);
        assertEquals(ExecutionSkipReason.MONTHLY_CAP, skipped.skipReason());

        AmountDecision exhausted = MandateAmounts.decide(AmountType.FIXED, new BigDecimal("10.00"), null,
                null, null, new BigDecimal("100.00"), new BigDecimal("100.00"), CapBehaviour.CAP);
        assertEquals(ExecutionSkipReason.MONTHLY_CAP, exhausted.skipReason());
    }

    @Test
    void minimumRejectsACappedRemainder() {
        AmountDecision decision = MandateAmounts.decide(AmountType.FIXED, new BigDecimal("30.00"), null,
                new BigDecimal("25.00"), null, new BigDecimal("80.00"), new BigDecimal("100.00"), CapBehaviour.CAP);
        assertEquals(ExecutionSkipReason.BELOW_MINIMUM, decision.skipReason());
    }

    @Test
    void missingOrNonPositiveConfigurationSkips() {
        assertEquals(ExecutionSkipReason.ZERO_AMOUNT, MandateAmounts.decide(AmountType.FIXED, null, null,
                null, null, null, null, CapBehaviour.SKIP).skipReason());
        assertEquals(ExecutionSkipReason.ZERO_AMOUNT, MandateAmounts.decide(AmountType.FIXED, BigDecimal.ZERO, null,
                null, null, null, null, CapBehaviour.SKIP).skipReason());
    }
}
