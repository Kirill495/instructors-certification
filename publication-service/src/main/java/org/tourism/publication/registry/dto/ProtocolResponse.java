package org.tourism.publication.registry.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record ProtocolResponse(
        String number,
        LocalDate date,
        String orderNumber,
        List<AssignmentResponse> assignments) {
    public ProtocolResponse {
        assignments = List.copyOf(assignments);
    }
}
