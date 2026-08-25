package org.tourism.publication.registry.dto;

import java.time.LocalDate;

/**
 * @param validUntil - @nullable для присвоений бессрочных разрядов
 */
public record AssignmentResponse(
        int rowNum,
        String lastName,
        String firstName,
        String middleName,
        String grade,
        String kindOfTourism,
        String club,
        LocalDate assignmentDate,
        LocalDate validUntil) {}
