package org.tourism.instructors.domain.protocol;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.tourism.instructors.domain.catalog.model.Grade;
import org.tourism.instructors.domain.catalog.model.KindOfTourism;
import org.tourism.instructors.domain.tourist.model.Tourist;

@Entity
@Table(name = "protocols_content", schema = "instructors_grades")
@Getter
@Setter
@NoArgsConstructor
public class ProtocolContent {

    @EmbeddedId
    private ProtocolContentPk id;

    @MapsId("protocolId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "protocol_id", insertable = false, updatable = false)
    private Protocol protocol;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tourist_id", nullable = false)
    private Tourist tourist;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kind_of_tourism", nullable = false)
    private KindOfTourism kindOfTourism;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grade", nullable = false)
    private Grade grade;

    @Column(name = "certification_id")
    private String certificationID;

    @Column(name = "club")
    private String club;

    @Column(name = "decision_type")
    private String decisionType;
}
