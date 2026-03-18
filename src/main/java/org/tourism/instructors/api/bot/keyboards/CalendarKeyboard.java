package org.tourism.instructors.api.bot.keyboards;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CalendarKeyboard {

    private static final DateTimeFormatter HEADER_FORMAT =
            DateTimeFormatter.ofPattern("LLLL yyyy", Locale.of("ru"));
    private static final String IGNORE_CALLBACK_DATA = "cal:IGNORE";

    private CalendarKeyboard() {
    }

    public static InlineKeyboardMarkup build(YearMonth yearMonth) {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // Header: month + year (no navigation — already chosen)
        String header = yearMonth.format(HEADER_FORMAT);
        header = Character.toUpperCase(header.charAt(0)) + header.substring(1);
        rows.add(List.of(button(header, IGNORE_CALLBACK_DATA)));

        // Day-of-week headers
        rows.add(List.of(
                button("Пн", IGNORE_CALLBACK_DATA), button("Вт", IGNORE_CALLBACK_DATA),
                button("Ср", IGNORE_CALLBACK_DATA), button("Чт", IGNORE_CALLBACK_DATA),
                button("Пт", IGNORE_CALLBACK_DATA), button("Сб", IGNORE_CALLBACK_DATA),
                button("Вс", IGNORE_CALLBACK_DATA)
        ));

        // Day grid
        LocalDate firstDay = yearMonth.atDay(1);
        int startDayOfWeek = firstDay.getDayOfWeek().getValue();

        List<InlineKeyboardButton> week = new ArrayList<>();
        for (int i = 1; i < startDayOfWeek; i++) {
            week.add(button(" ", IGNORE_CALLBACK_DATA));
        }

        for (int day = 1; day <= yearMonth.lengthOfMonth(); day++) {
            LocalDate date = yearMonth.atDay(day);
            week.add(button(String.valueOf(day), "cal:SELECT:" + date));
            if (date.getDayOfWeek().getValue() == 7) {
                rows.add(new ArrayList<>(week));
                week.clear();
            }
        }

        if (!week.isEmpty()) {
            while (week.size() < 7) week.add(button(" ", IGNORE_CALLBACK_DATA));
            rows.add(week);
        }

        // Back to month picker
        rows.add(List.of(button("← К месяцам", "cal:SELECT_YEAR:" + yearMonth.getYear())));

        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    private static InlineKeyboardButton button(String text, String callbackData) {
        return InlineKeyboardButton.builder().text(text).callbackData(callbackData).build();
    }
}