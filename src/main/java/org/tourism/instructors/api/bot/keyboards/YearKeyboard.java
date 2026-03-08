package org.tourism.instructors.api.bot.keyboards;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;

public class YearKeyboard {

    public static final int PAGE_SIZE = 16; // 4 columns × 4 rows

    public static InlineKeyboardMarkup build(int startYear) {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // Navigation row
        rows.add(List.of(
                button("◄", "cal:YEAR_PAGE:" + (startYear - PAGE_SIZE)),
                button(startYear + " – " + (startYear + PAGE_SIZE - 1), "cal:IGNORE"),
                button("►", "cal:YEAR_PAGE:" + (startYear + PAGE_SIZE))
        ));

        // Year grid (4 columns × 4 rows)
        List<InlineKeyboardButton> row = new ArrayList<>();
        for (int year = startYear; year < startYear + PAGE_SIZE; year++) {
            row.add(button(String.valueOf(year), "cal:SELECT_YEAR:" + year));
            if (row.size() == 4) {
                rows.add(new ArrayList<>(row));
                row.clear();
            }
        }

        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    /** Returns the page start year for a given year (aligns to PAGE_SIZE boundary). */
    public static int pageStartFor(int year) {
        return (year / PAGE_SIZE) * PAGE_SIZE;
    }

    private static InlineKeyboardButton button(String text, String callbackData) {
        return InlineKeyboardButton.builder().text(text).callbackData(callbackData).build();
    }
}