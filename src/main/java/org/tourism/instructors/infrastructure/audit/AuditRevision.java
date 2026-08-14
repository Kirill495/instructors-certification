package org.tourism.instructors.infrastructure.audit;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.RevisionEntity;
import org.hibernate.envers.RevisionNumber;
import org.hibernate.envers.RevisionTimestamp;

@Entity
@RevisionEntity(AuditRevisionListener.class)
@Setter
@Getter
@Table(name = "revinfo", schema = "instructors_grades")
public class AuditRevision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @RevisionNumber
    @Column(name = "rev")
    private int rev;

    @RevisionTimestamp
    @Column(name = "revtstmp")
    private LocalDateTime revtstmp;

    @Column(name = "username")
    private String username;

    @Column(name = "operation_source")
    @Enumerated(EnumType.STRING)
    private OperationSource operationSource;
}
