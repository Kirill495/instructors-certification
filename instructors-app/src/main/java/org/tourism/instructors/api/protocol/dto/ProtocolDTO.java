package org.tourism.instructors.api.protocol.dto;

import java.time.LocalDate;
import java.util.List;
import org.tourism.instructors.domain.protocol.ProtocolStatus;

public record ProtocolDTO(
        Integer id,
        String number,
        LocalDate date,
        String order,
        ProtocolStatus status,
        List<ProtocolContentDTO> contentRows) {}
