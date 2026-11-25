package io.github.hossensyedriadh.cbs.mandate.internal.util;

import io.github.hossensyedriadh.cbs.mandate.api.enumerator.HolidayAdjustment;

import java.time.LocalDate;
import java.util.Optional;

public interface BankHolidayCalendar {
    boolean isBusinessDay(LocalDate date);

    Optional<LocalDate> shift(LocalDate date, HolidayAdjustment adjustment);
}
