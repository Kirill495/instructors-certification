package org.tourism.instructors.api.protocol.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;
import org.tourism.instructors.api.tourist.dto.TouristLightDTO;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Setter
@Getter
@NoArgsConstructor
public class ProtocolForListDTO {
    private int id;
    private String number;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;
    private String order;
    private List<TouristLightDTO> tourists = new ArrayList<>();

    // In ProtocolForListDTO.java
    public String getTooltipHtml() {
        if (getTourists().size() <= 2) return "";
        return getTourists().stream()
                       .skip(2)
                       .map(t -> "<li>" + t.getFullName() + "</li>")
                       .collect(Collectors.joining("", "<ul class=\"mb-0 ps-3\">", "</ul>"));
    }
}
