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
    private Integer protocolId;
    private Integer rowNum;
    private Integer touristId;
    private Integer kindOfTourismId;
    private Integer gradeId;
    private String certificationId;
    private String club;

    private String touristFullName;
    private String kindOfTourismTitle;
    private String gradeTitle;
}
