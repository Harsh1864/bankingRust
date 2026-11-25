package io.github.hossensyedriadh.cbs.mandate.internal.util;

import io.github.hossensyedriadh.cbs.mandate.api.enumerator.PaymentFrequency;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
public class ScheduleCalculator {
    public LocalDate firstAnchor(LocalDate start, PaymentFrequency frequency, Integer dayOfMonth, Integer dayOfWeek) {
        return switch (frequency) {
            case DAILY -> start;
            case WEEKLY, FORTNIGHTLY -> onOrAfterWeekday(start, dayOfWeek == null ? start.getDayOfWeek().getValue() : dayOfWeek);
            case MONTHLY -> onOrAfterMonthDay(start, dayOfMonth == null ? start.getDayOfMonth() : dayOfMonth, 1);
            case QUARTERLY -> onOrAfterMonthDay(start, dayOfMonth == null ? start.getDayOfMonth() : dayOfMonth, 3);
            case SEMI_ANNUAL -> onOrAfterMonthDay(start, dayOfMonth == null ? start.getDayOfMonth() : dayOfMonth, 6);
            case ANNUAL -> onOrAfterMonthDay(start, dayOfMonth == null ? start.getDayOfMonth() : dayOfMonth, 12);
        };
    }

    public LocalDate advance(LocalDate anchor, PaymentFrequency frequency, Integer dayOfMonth) {
        int monthDay = dayOfMonth == null ? anchor.getDayOfMonth() : dayOfMonth;
        return switch (frequency) {
            case DAILY -> anchor.plusDays(1);
            case WEEKLY -> anchor.plusWeeks(1);
            case FORTNIGHTLY -> anchor.plusWeeks(2);
            case MONTHLY -> plusMonths(anchor, 1, monthDay);
            case QUARTERLY -> plusMonths(anchor, 3, monthDay);
            case SEMI_ANNUAL -> plusMonths(anchor, 6, monthDay);
            case ANNUAL -> plusMonths(anchor, 12, monthDay);
        };
    }

    public List<LocalDate> preview(LocalDate anchor, LocalDate end, PaymentFrequency frequency, Integer dayOfMonth, int occurrences) {
        List<LocalDate> dates = new ArrayList<>();
        LocalDate cursor = anchor;
        int guard = 0;
        while (cursor != null && dates.size() < occurrences && guard < 500) {
            if (end != null && cursor.isAfter(end)) {
                break;
            }
            dates.add(cursor);
            cursor = advance(cursor, frequency, dayOfMonth);
            guard++;
        }
        return dates;
    }

    private LocalDate onOrAfterWeekday(LocalDate start, int isoDay) {
        DayOfWeek wanted = DayOfWeek.of(isoDay);
        int delta = wanted.getValue() - start.getDayOfWeek().getValue();
        if (delta < 0) {
            delta += 7;
        }
        return start.plusDays(delta);
    }

    private LocalDate onOrAfterMonthDay(LocalDate start, int dayOfMonth, int stepMonths) {
        int day = Math.min(dayOfMonth, start.lengthOfMonth());
        LocalDate candidate = start.withDayOfMonth(day);
        if (!candidate.isBefore(start)) {
            return candidate;
        }
        return plusMonths(candidate, stepMonths, dayOfMonth);
    }

    private LocalDate plusMonths(LocalDate anchor, int months, int dayOfMonth) {
        LocalDate shifted = anchor.withDayOfMonth(1).plusMonths(months);
        int day = Math.min(dayOfMonth, shifted.lengthOfMonth());
        return shifted.withDayOfMonth(day);
    }
}
