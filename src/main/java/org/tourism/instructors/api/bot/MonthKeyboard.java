package org.tourism.instructors.api.bot;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

public class MonthKeyboard {

    private static final String[] MONTHS = {
            "Январь", "Февраль", "Март",
            "Апрель", "Май",     "Июнь",
            "Июль",   "Август",  "Сентябрь",
            "Октябрь","Ноябрь",  "Декабрь"
    };

    public static InlineKeyboardMarkup build(int year) {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // Header: selected year
        rows.add(List.of(button(String.valueOf(year), "cal:IGNORE")));

        // Month grid (3 columns × 4 rows)
        List<InlineKeyboardButton> row = new ArrayList<>();
        for (int month = 1; month <= 12; month++) {
            String ym = YearMonth.of(year, month).toString(); // "1990-03"
            row.add(button(MONTHS[month - 1], "cal:SELECT_MONTH:" + ym));
            if (row.size() == 3) {
                rows.add(new ArrayList<>(row));
                row.clear();
            }
        }

        // Back to year picker
        rows.add(List.of(button("← К годам", "cal:YEAR_PAGE:" + YearKeyboard.pageStartFor(year))));

        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    private static InlineKeyboardButton button(String text, String callbackData) {
        return InlineKeyboardButton.builder().text(text).callbackData(callbackData).build();
    }
}