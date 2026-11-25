package io.github.hossensyedriadh.cbs.mandate.internal.util;

import io.github.hossensyedriadh.cbs.mandate.api.enumerator.PaymentFrequency;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScheduleCalculatorTest {
    private final ScheduleCalculator calculator = new ScheduleCalculator();

    @Test
    void dailyAdvancesOneDayAtATime() {
        LocalDate start = LocalDate.of(2026, 11, 25);
        assertEquals(start, this.calculator.firstAnchor(start, PaymentFrequency.DAILY, null, null));
        assertEquals(start.plusDays(1), this.calculator.advance(start, PaymentFrequency.DAILY, null));
    }

    @Test
    void weeklyUsesTheNextRequestedWeekday() {
        LocalDate wednesday = LocalDate.of(2026, 11, 25);
        LocalDate monday = this.calculator.firstAnchor(wednesday, PaymentFrequency.WEEKLY, null, 1);
        assertEquals(LocalDate.of(2026, 11, 30), monday);
        assertEquals(monday.plusWeeks(1), this.calculator.advance(monday, PaymentFrequency.WEEKLY, null));
    }

    @Test
    void weeklyStaysPutWhenTheStartIsAlreadyThatWeekday() {
        LocalDate monday = LocalDate.of(2026, 11, 30);
        assertEquals(monday, this.calculator.firstAnchor(monday, PaymentFrequency.WEEKLY, null, 1));
    }

    @Test
    void fortnightlyKeepsATwoWeekGap() {
        LocalDate start = LocalDate.of(2026, 1, 5);
        LocalDate first = this.calculator.firstAnchor(start, PaymentFrequency.FORTNIGHTLY, null, 1);
        LocalDate second = this.calculator.advance(first, PaymentFrequency.FORTNIGHTLY, null);
        assertEquals(14, second.toEpochDay() - first.toEpochDay());
    }

    @Test
    void monthlyClampsShortMonthsAndRestoresTheRequestedDay() {
        LocalDate january = LocalDate.of(2026, 1, 31);
        LocalDate february = this.calculator.advance(january, PaymentFrequency.MONTHLY, 31);
        LocalDate march = this.calculator.advance(february, PaymentFrequency.MONTHLY, 31);
        assertEquals(LocalDate.of(2026, 2, 28), february);
        assertEquals(LocalDate.of(2026, 3, 31), march);
    }

    @Test
    void leapYearKeepsFebruaryTwentyNineWhenTheDayExists() {
        LocalDate january = LocalDate.of(2028, 1, 31);
        assertEquals(LocalDate.of(2028, 2, 29), this.calculator.advance(january, PaymentFrequency.MONTHLY, 31));
    }

    @Test
    void firstMonthlyDateMovesForwardWhenTheDayHasPassed() {
        LocalDate start = LocalDate.of(2026, 1, 31);
        assertEquals(LocalDate.of(2026, 2, 15), this.calculator.firstAnchor(start, PaymentFrequency.MONTHLY, 15, null));
    }

    @Test
    void quarterlyStepsThreeMonths() {
        LocalDate start = LocalDate.of(2026, 1, 15);
        assertEquals(LocalDate.of(2026, 4, 15), this.calculator.advance(start, PaymentFrequency.QUARTERLY, 15));
    }

    @Test
    void annualCrossesTheYearBoundary() {
        LocalDate start = LocalDate.of(2026, 11, 25);
        assertEquals(LocalDate.of(2027, 11, 25), this.calculator.advance(start, PaymentFrequency.ANNUAL, 25));
    }

    @Test
    void previewStopsAtTheEndDateAndTheRequestedCount() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        List<LocalDate> dates = this.calculator.preview(start, LocalDate.of(2026, 1, 4), PaymentFrequency.DAILY, null, 12);
        assertEquals(4, dates.size());
        assertEquals(start, dates.get(0));
        assertEquals(LocalDate.of(2026, 1, 4), dates.get(3));

        List<LocalDate> capped = this.calculator.preview(start, null, PaymentFrequency.MONTHLY, 1, 3);
        assertEquals(3, capped.size());
        assertFalse(capped.get(2).isBefore(capped.get(1)));
        assertTrue(capped.get(1).isAfter(capped.get(0)));
    }
}
