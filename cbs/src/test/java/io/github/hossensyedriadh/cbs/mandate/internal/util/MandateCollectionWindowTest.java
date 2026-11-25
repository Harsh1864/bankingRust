package io.github.hossensyedriadh.cbs.mandate.internal.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MandateCollectionWindowTest {
    @Test
    void runsWhenTheBusinessDateIsTodayOrWithinTheLag() {
        LocalDate today = LocalDate.of(2026, 11, 25);
        assertTrue(MandateCollectionWindow.shouldRun(today, today));
        assertTrue(MandateCollectionWindow.shouldRun(today.minusDays(7), today));
        assertNull(MandateCollectionWindow.holdReason(today, today));
    }

    @Test
    void holdsWhenTheBusinessDateIsInTheFutureOrTooFarBehind() {
        LocalDate today = LocalDate.of(2026, 11, 25);
        assertFalse(MandateCollectionWindow.shouldRun(today.plusDays(1), today));
        assertFalse(MandateCollectionWindow.shouldRun(today.minusDays(8), today));
        assertEquals("Business date is ahead of the system date", MandateCollectionWindow.holdReason(today.plusDays(1), today));
        assertEquals("Business date is not available", MandateCollectionWindow.holdReason(null, today));
    }
}
