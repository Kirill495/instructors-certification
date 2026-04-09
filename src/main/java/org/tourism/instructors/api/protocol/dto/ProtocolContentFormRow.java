package org.tourism.instructors.api.protocol.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProtocolContentFormRow {
    private Integer protocolId;
    private Integer rowNum;
    private Integer touristId;
    private String touristTitle;
    private Integer kindOfTourismId;
    private Integer gradeId;
    private String certificationId;
    private String club;
}
