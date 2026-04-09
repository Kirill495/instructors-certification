package org.tourism.instructors.api.protocol.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;
import org.tourism.instructors.domain.protocol.ProtocolStatus;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ProtocolFormDTO {
    private Integer id;
    private String number;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;
    private String order;
    private ProtocolStatus status;
    private List<ProtocolContentFormRow> contentRows = new ArrayList<>();

    public boolean isNewItem() {
        return id == null;
    }
}