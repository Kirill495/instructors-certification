package org.tourism.instructors.application.protocol.mapper;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import org.springframework.stereotype.Component;
import org.tourism.instructors.domain.protocol.Protocol;
import org.tourism.publication.contract.AssignmentSnapshot;
import org.tourism.publication.contract.ProtocolSnapshot;

@Component
public class ProtocolSnapshotMapper {
    private static final int SNAPSHOT_VERSION = 1;

    private final Clock clock;

    public ProtocolSnapshotMapper(Clock clock) {
        this.clock = clock;
    }

    public ProtocolSnapshot toSnapshot(Protocol protocol) {
        return new ProtocolSnapshot(
                SNAPSHOT_VERSION,
                protocol.getId(),
                protocol.getNumber(),
                protocol.getDate(),
                protocol.getOrder(),
                Instant.now(clock),
                protocol.getProtocolContents().stream()
                        .map(
                                item ->
                                        new AssignmentSnapshot(
                                                item.getId().getRowNum(),
                                                item.getTourist().getLastName(),
                                                item.getTourist().getFirstName(),
                                                item.getTourist().getMiddleName(),
                                                item.getGrade().getTitle(),
                                                item.getKindOfTourism().getTitle(),
                                                item.getClub(),
                                                protocol.getDate(),
                                                (item.getGrade().getExpiresInYears() > 0)
                                                        ? protocol.getDate()
                                                                .plusYears(
                                                                        item.getGrade()
                                                                                .getExpiresInYears())
                                                        : null))
                        .sorted(Comparator.comparing(AssignmentSnapshot::rowNum))
                        .toList());
    }
}
