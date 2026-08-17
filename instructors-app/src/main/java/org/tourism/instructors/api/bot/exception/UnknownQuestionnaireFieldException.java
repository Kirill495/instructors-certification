package org.tourism.instructors.api.bot.exception;

public class UnknownQuestionnaireFieldException extends RuntimeException {
    public UnknownQuestionnaireFieldException(String field) {
        super("Неизвестное поле анкеты: " + field);
    }
}
