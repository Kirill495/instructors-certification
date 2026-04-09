package org.tourism.instructors.api.protocol.dto;

import org.tourism.instructors.api.tourist.dto.TouristSummaryDTO;
import org.tourism.instructors.domain.protocol.ProtocolStatus;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public record ProtocolForListDTO (
    int id,
    String number,
    LocalDate date,
    String order,
    ProtocolStatus status,
    List<TouristSummaryDTO> tourists) {

    public ProtocolForListDTO {
        tourists = tourists == null ? new ArrayList<>() : tourists;
    }

    public String getTooltipHtml() {
        if (tourists.size() <= 2) return "";
        return tourists().stream()
                       .skip(2)
                       .map(t -> "<li>" + t.getFullName() + "</li>")
                       .collect(Collectors.joining("", "<ul class=\"mb-0 ps-3\">", "</ul>"));
    }
}
