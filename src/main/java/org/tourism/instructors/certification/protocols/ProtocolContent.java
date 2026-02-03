package org.tourism.instructors.certification.protocols;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.tourism.instructors.certification.common.Grade;
import org.tourism.instructors.certification.common.KindOfTourism;
import org.tourism.instructors.certification.tourists.Tourist;

@Entity
@Table(name = "protocols_content")
@Getter
@Setter
@NoArgsConstructor
public class ProtocolContent {

    @EmbeddedId
    private ProtocolContentPk id;

    @MapsId("protocolId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id", insertable = false, updatable = false)
    private Protocol protocol;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id", nullable = false)
    private Tourist tourist;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id", nullable = false)
    private KindOfTourism kindOfTourism;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id", nullable = false)
    private Grade grade;

    @Column(name = "cert_id")
    private String certificationID;
}
