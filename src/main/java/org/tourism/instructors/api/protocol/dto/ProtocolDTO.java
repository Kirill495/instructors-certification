package org.tourism.instructors.api.protocol.dto;

import org.tourism.instructors.domain.protocol.ProtocolStatus;

import java.time.LocalDate;
import java.util.List;

public record ProtocolDTO(
        Integer id,
        String number,
        LocalDate date,
        String order,
        ProtocolStatus status,
        List<ProtocolContentDTO> contentRows) {
}