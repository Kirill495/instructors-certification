package org.tourism.instructors.api.bot.exception;

public class UnknownTelegramOptionException extends RuntimeException {
    public UnknownTelegramOptionException(String option) {
        super("Unknown option: " + option);
    }
}
