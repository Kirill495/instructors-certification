package org.tourism.publication.contract;

import java.time.LocalDate;

/**
 * @param validUntil - @nullable для присвоений бессрочных разрядов
 */
public record AssignmentSnapshot(
        int rowNum,
        String lastName,
        String firstName,
        String middleName,
        String grade,
        String kindOfTourism,
        String club,
        LocalDate assignmentDate,
        LocalDate validUntil) {}
