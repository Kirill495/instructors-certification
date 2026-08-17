package org.tourism.instructors.api.bot.exception;

public class UnknownButtonConversationException extends RuntimeException {

    public UnknownButtonConversationException(String key) {
        super("Неизвестная кнопка: " + key);
    }
}
