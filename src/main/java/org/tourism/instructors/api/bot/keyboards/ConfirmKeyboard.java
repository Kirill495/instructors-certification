package org.tourism.instructors.api.bot.keyboards;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;

public class ConfirmKeyboard {

    public static InlineKeyboardMarkup build() {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // Navigation row
        rows.add(List.of(
                button("Все верно", "CONFIRM:OK"),
                button("Исправить", "CONFIRM:Cancel")
        ));
        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    private static InlineKeyboardButton button(String text, String callbackData) {
        return InlineKeyboardButton.builder().text(text).callbackData(callbackData).build();
    }
}