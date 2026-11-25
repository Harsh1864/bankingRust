package io.github.hossensyedriadh.cbs.mandate.internal.util;

import io.github.hossensyedriadh.cbs.mandate.api.enumerator.HolidayAdjustment;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultBankHolidayCalendarTest {
    private final DefaultBankHolidayCalendar calendar = new DefaultBankHolidayCalendar();

    @Test
    void weekendsAreNotBusinessDays() {
        assertFalse(this.calendar.isBusinessDay(LocalDate.of(2026, 11, 28)));
        assertFalse(this.calendar.isBusinessDay(LocalDate.of(2026, 11, 29)));
        assertTrue(this.calendar.isBusinessDay(LocalDate.of(2026, 11, 26)));
    }

    @Test
    void easterSundayMatchesKnownYears() {
        assertEquals(LocalDate.of(2024, 3, 31), DefaultBankHolidayCalendar.easterSunday(2024));
        assertEquals(LocalDate.of(2025, 4, 20), DefaultBankHolidayCalendar.easterSunday(2025));
        assertEquals(LocalDate.of(2026, 4, 5), DefaultBankHolidayCalendar.easterSunday(2026));
    }

    @Test
    void goodFridayAndEasterMondayAreClosed() {
        assertFalse(this.calendar.isBusinessDay(LocalDate.of(2026, 4, 3)));
        assertFalse(this.calendar.isBusinessDay(LocalDate.of(2026, 4, 6)));
        assertTrue(this.calendar.isBusinessDay(LocalDate.of(2026, 4, 7)));
    }

    @Test
    void christmasAndBoxingDayObservedWhenTheyFallOnAWeekend() {
        assertTrue(this.calendar.holidays(2021).contains(LocalDate.of(2021, 12, 27)));
        assertTrue(this.calendar.holidays(2021).contains(LocalDate.of(2021, 12, 28)));
        assertFalse(this.calendar.isBusinessDay(LocalDate.of(2021, 12, 27)));
        assertFalse(this.calendar.isBusinessDay(LocalDate.of(2021, 12, 28)));
    }

    @Test
    void newYearObservedOnMondayWhenItFallsOnSaturday() {
        assertTrue(this.calendar.holidays(2022).contains(LocalDate.of(2022, 1, 3)));
        assertFalse(this.calendar.isBusinessDay(LocalDate.of(2022, 1, 3)));
    }

    @Test
    void followingRollsToTheNextBusinessDayAndSkipDeclines() {
        LocalDate saturday = LocalDate.of(2026, 11, 28);
        assertEquals(LocalDate.of(2026, 11, 30), this.calendar.shift(saturday, HolidayAdjustment.FOLLOWING).orElseThrow());
        assertEquals(LocalDate.of(2026, 11, 27), this.calendar.shift(saturday, HolidayAdjustment.PRECEDING).orElseThrow());
        assertEquals(Optional.empty(), this.calendar.shift(saturday, HolidayAdjustment.SKIP));
        assertEquals(saturday, this.calendar.shift(saturday, HolidayAdjustment.NONE).orElseThrow());
    }

    @Test
    void followingDoesNotMoveANormalBusinessDay() {
        LocalDate thursday = LocalDate.of(2026, 11, 26);
        assertEquals(thursday, this.calendar.shift(thursday, HolidayAdjustment.FOLLOWING).orElseThrow());
    }
}
