package org.tourism.instructors.application.protocol.mapper;

import org.tourism.instructors.domain.protocol.Protocol;
import org.tourism.publication.contract.AssignmentSnapshot;
import org.tourism.publication.contract.ProtocolSnapshot;

import java.time.ZoneOffset;
import java.util.Comparator;

public class ProtocolMapper {
    private static final int SNAPSHOT_VERSION = 1;
    public ProtocolSnapshot toSnapshot(Protocol protocol) {
        return new ProtocolSnapshot(
                SNAPSHOT_VERSION,
                        protocol.getId(),
                        protocol.getNumber(),
                        protocol.getDate(),
                        protocol.getOrder(),
                        protocol.getDate().atStartOfDay().toInstant(ZoneOffset.UTC),
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
