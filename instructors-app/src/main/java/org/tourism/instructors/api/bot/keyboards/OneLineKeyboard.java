package org.tourism.instructors.api.bot.keyboards;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

public class OneLineKeyboard {

    private OneLineKeyboard() {}

    public static InlineKeyboardMarkup build(Supplier<Map<String, String>> buttonSupplier) {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        rows.add(
                buttonSupplier.get().entrySet().stream()
                        .map(e -> button(e.getKey(), e.getValue()))
                        .toList());
        // Navigation row
        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    private static InlineKeyboardButton button(String text, String callbackData) {
        return InlineKeyboardButton.builder().text(text).callbackData(callbackData).build();
    }
}
