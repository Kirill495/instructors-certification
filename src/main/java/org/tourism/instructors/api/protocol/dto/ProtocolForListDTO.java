package org.tourism.instructors.api.protocol.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;
import org.tourism.instructors.api.tourist.dto.TouristLightDTO;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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
}
