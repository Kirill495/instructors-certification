package org.tourism.instructors.domain.protocol;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class ProtocolContentPk implements Serializable {
    @Column(name = "protocol_id")
    private int protocolId;

    @Column(name = "row_num")
    private int rowNum;
}
