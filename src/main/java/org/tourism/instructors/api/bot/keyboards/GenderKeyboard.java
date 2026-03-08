package org.tourism.instructors.api.bot.keyboards;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;

public class GenderKeyboard {

    public static InlineKeyboardMarkup build() {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // Navigation row
        rows.add(List.of(
                button("Мужской", "GENDER:MALE"),
                button("Женский", "GENDER:FEMALE")
        ));
        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    private static InlineKeyboardButton button(String text, String callbackData) {
        return InlineKeyboardButton.builder().text(text).callbackData(callbackData).build();
    }
}