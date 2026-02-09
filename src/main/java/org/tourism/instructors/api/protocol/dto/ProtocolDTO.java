package org.tourism.instructors.api.protocol.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ProtocolDTO {
    private int id;
    private String number;
    private LocalDate date;
    private String order;
    private List<ProtocolContentDTO> contentRows = new ArrayList<>();
}

