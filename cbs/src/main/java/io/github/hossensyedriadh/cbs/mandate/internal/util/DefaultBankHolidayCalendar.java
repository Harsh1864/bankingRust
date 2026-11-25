package io.github.hossensyedriadh.cbs.mandate.internal.util;

import io.github.hossensyedriadh.cbs.mandate.api.enumerator.HolidayAdjustment;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class DefaultBankHolidayCalendar implements BankHolidayCalendar {
    private static final int MAX_ROLL_DAYS = 14;

    private final ConcurrentHashMap<Integer, Set<LocalDate>> holidaysByYear = new ConcurrentHashMap<>();

    @Override
    public boolean isBusinessDay(LocalDate date) {
        DayOfWeek day = date.getDayOfWeek();
        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
            return false;
        }
        return !holidays(date.getYear()).contains(date);
    }

    @Override
    public Optional<LocalDate> shift(LocalDate date, HolidayAdjustment adjustment) {
        if (date == null || adjustment == null || adjustment == HolidayAdjustment.NONE) {
            return Optional.ofNullable(date);
        }
        if (adjustment == HolidayAdjustment.SKIP) {
            return isBusinessDay(date) ? Optional.of(date) : Optional.empty();
        }
        int step = adjustment == HolidayAdjustment.PRECEDING ? -1 : 1;
        return Optional.of(roll(date, step));
    }

    public Set<LocalDate> holidays(int year) {
        return holidaysByYear.computeIfAbsent(year, this::computeHolidays);
    }

    public static LocalDate easterSunday(int year) {
        int a = year % 19;
        int b = year / 100;
        int c = year % 100;
        int d = b / 4;
        int e = b % 4;
        int f = (b + 8) / 25;
        int g = (b - f + 1) / 3;
        int h = (19 * a + b - d - g + 15) % 30;
        int i = c / 4;
        int k = c % 4;
        int l = (32 + 2 * e + 2 * i - h - k) % 7;
        int m = (a + 11 * h + 22 * l) / 451;
        int month = (h + l - 7 * m + 114) / 31;
        int day = ((h + l - 7 * m + 114) % 31) + 1;
        return LocalDate.of(year, month, day);
    }

    private Set<LocalDate> computeHolidays(int year) {
        Set<LocalDate> holidays = new HashSet<>();
        LocalDate easter = easterSunday(year);
        holidays.add(easter.minusDays(2));
        holidays.add(easter.plusDays(1));
        addObserved(holidays, LocalDate.of(year, 1, 1));
        addObserved(holidays, LocalDate.of(year, 5, 1));
        addObserved(holidays, LocalDate.of(year, 12, 25));
        addObserved(holidays, LocalDate.of(year, 12, 26));
        return Set.copyOf(holidays);
    }

    private void addObserved(Set<LocalDate> holidays, LocalDate holiday) {
        LocalDate observed = observeWeekend(holiday);
        while (holidays.contains(observed)) {
            observed = observeWeekend(observed.plusDays(1));
        }
        holidays.add(observed);
    }

    private LocalDate observeWeekend(LocalDate holiday) {
        return switch (holiday.getDayOfWeek()) {
            case SATURDAY -> holiday.plusDays(2);
            case SUNDAY -> holiday.plusDays(1);
            default -> holiday;
        };
    }

    private LocalDate roll(LocalDate date, int step) {
        LocalDate cursor = date;
        for (int i = 0; i < MAX_ROLL_DAYS; i++) {
            if (isBusinessDay(cursor)) {
                return cursor;
            }
            cursor = cursor.plusDays(step);
        }
        return date;
    }
}
