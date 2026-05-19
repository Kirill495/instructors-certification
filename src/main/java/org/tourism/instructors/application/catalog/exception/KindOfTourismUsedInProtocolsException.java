package org.tourism.instructors.application.catalog.exception;

import org.tourism.instructors.domain.catalog.model.KindOfTourism;

public class KindOfTourismUsedInProtocolsException extends KindOfTourismCannotBeDeletedException {
    public KindOfTourismUsedInProtocolsException(KindOfTourism kindOfTourism) {
        super("Вид туризма " + kindOfTourism.getTitle() + " не может быть удален. Используется в протоколах", kindOfTourism.getId());
    }
}
