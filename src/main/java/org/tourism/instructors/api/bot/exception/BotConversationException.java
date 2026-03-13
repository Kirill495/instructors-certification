package org.tourism.instructors.api.bot.exception;

public class BotConversationException extends RuntimeException {

    public BotConversationException(String key) {
        super("Неизвестная кнопка: " + key);
    }
}
