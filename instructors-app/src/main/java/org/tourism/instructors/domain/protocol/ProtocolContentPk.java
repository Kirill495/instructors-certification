package org.tourism.instructors.domain.protocol;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
