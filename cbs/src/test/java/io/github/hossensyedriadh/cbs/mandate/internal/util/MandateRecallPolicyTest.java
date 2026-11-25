package io.github.hossensyedriadh.cbs.mandate.internal.util;

import io.github.hossensyedriadh.cbs.mandate.api.enumerator.ExecutionStatus;
import io.github.hossensyedriadh.cbs.mandate.api.enumerator.HolidayAdjustment;
import io.github.hossensyedriadh.cbs.mandate.api.enumerator.MandateKind;
import io.github.hossensyedriadh.cbs.mandate.api.enumerator.MandateRecallReason;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MandateRecallPolicyTest {
    private final BankHolidayCalendar calendar = new DefaultBankHolidayCalendar();

    @Test
    void standingOrderCanBeRecalledInsideFiveBusinessDays() {
        LocalDate collected = LocalDate.of(2026, 11, 23);
        LocalDate today = LocalDate.of(2026, 11, 26);
        RecallDecision decision = MandateRecallPolicy.evaluate(MandateKind.STANDING_ORDER, ExecutionStatus.POSTED, 90L,
                collected, today, MandateRecallReason.PAYER_REQUEST, this.calendar);
        assertTrue(decision.allowed());
        assertEquals(3, decision.businessDaysElapsed());
    }

    @Test
    void standingOrderCannotUseADirectDebitReason() {
        RecallDecision decision = MandateRecallPolicy.evaluate(MandateKind.STANDING_ORDER, ExecutionStatus.POSTED, 90L,
                LocalDate.of(2026, 11, 26), LocalDate.of(2026, 11, 26), MandateRecallReason.NO_AUTHORITY, this.calendar);
        assertFalse(decision.allowed());
    }

    @Test
    void directDebitRejectsAPayerRequestAndAcceptsNoAuthority() {
        LocalDate collected = LocalDate.of(2026, 11, 20);
        assertFalse(MandateRecallPolicy.evaluate(MandateKind.DIRECT_DEBIT, ExecutionStatus.POSTED, 91L, collected,
                LocalDate.of(2026, 11, 26), MandateRecallReason.PAYER_REQUEST, this.calendar).allowed());
        assertTrue(MandateRecallPolicy.evaluate(MandateKind.DIRECT_DEBIT, ExecutionStatus.POSTED, 91L, collected,
                LocalDate.of(2026, 11, 26), MandateRecallReason.NO_AUTHORITY, this.calendar).allowed());
    }

    @Test
    void sweepIsOnlyAllowedOnTheCollectionDate() {
        LocalDate friday = LocalDate.of(2026, 11, 27);
        assertTrue(MandateRecallPolicy.evaluate(MandateKind.SWEEP, ExecutionStatus.POSTED, 92L, friday, friday,
                MandateRecallReason.DUPLICATE, this.calendar).allowed());
        assertFalse(MandateRecallPolicy.evaluate(MandateKind.SWEEP, ExecutionStatus.POSTED, 92L, friday, friday.plusDays(3),
                MandateRecallReason.DUPLICATE, this.calendar).allowed());
    }

    @Test
    void postedStatusAndATransactionAreRequired() {
        LocalDate today = LocalDate.of(2026, 11, 26);
        assertFalse(MandateRecallPolicy.evaluate(MandateKind.STANDING_ORDER, ExecutionStatus.FAILED, 90L, today, today,
                MandateRecallReason.PAYER_REQUEST, this.calendar).allowed());
        assertFalse(MandateRecallPolicy.evaluate(MandateKind.STANDING_ORDER, ExecutionStatus.REVERSED, 90L, today, today,
                MandateRecallReason.PAYER_REQUEST, this.calendar).allowed());
        assertFalse(MandateRecallPolicy.evaluate(MandateKind.STANDING_ORDER, ExecutionStatus.POSTED, null, today, today,
                MandateRecallReason.PAYER_REQUEST, this.calendar).allowed());
    }

    @Test
    void weekendDoesNotConsumeTheStandingOrderWindow() {
        LocalDate friday = LocalDate.of(2026, 11, 27);
        LocalDate monday = LocalDate.of(2026, 11, 30);
        int elapsed = MandateRecallPolicy.businessDaysAfter(friday, monday, this.calendar);
        assertEquals(1, elapsed);
        assertTrue(this.calendar.shift(friday, HolidayAdjustment.NONE).isPresent());
    }
}
