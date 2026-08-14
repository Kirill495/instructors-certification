package org.tourism.instructors.application.catalog.exception;

import org.tourism.instructors.domain.catalog.model.Grade;

public class GradeUsedInProtocolsException extends GradeCannotBeDeletedException {
    public GradeUsedInProtocolsException(Grade grade) {
        super(
                "Звание "
                        + grade.getTitle()
                        + " не может быть удалено поскольку используется в протоколах",
                grade.getId());
    }
}
