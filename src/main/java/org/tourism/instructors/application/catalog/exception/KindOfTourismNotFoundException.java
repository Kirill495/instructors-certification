package org.tourism.instructors.application.catalog.exception;

public class KindOfTourismNotFoundException extends RuntimeException {
    public KindOfTourismNotFoundException(int id) {
        super("Вид туризма c ID:" + id + " не найден");
    }
}
