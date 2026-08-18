package org.tourism.publication.contract;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record ProtocolSnapshot(
        int version,
        int protocolId,
        String number,
        LocalDate date,
        String orderNumber,
        Instant publishedAt,
        List<AssignmentSnapshot> assignments) {
    public ProtocolSnapshot {
        assignments = List.copyOf(assignments);
    }
}
