package org.tourism.instructors.api.protocol.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProtocolContentDTO {
    private int protocolId;
    private int rowNum;
    private int touristId;
    private int kindOfTourismId;
    private int gradeId;
    private String certificationId;
    private String club;

    private String touristFullName;
    private String kindOfTourismTitle;
    private String gradeTitle;
}
