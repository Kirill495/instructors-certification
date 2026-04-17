package org.tourism.instructors.domain.tourist.model.contactinfo;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.Audited;
import org.tourism.instructors.domain.tourist.model.Tourist;
import tools.jackson.databind.ObjectMapper;

@Audited
@Entity
@Table(name = "contact_info")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ContactInfoItem {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "tourist_id", nullable = false)
    private Tourist tourist;

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    private ContactInfoType type;

    @Column(name = "value")
    private String value;

    @Column(name = "details")
    private String detailsJson;

    @Transient
    private ContactInfoDetails details;

    @PostLoad
    private void deserializeDetails() {
        if (detailsJson != null && type.getDetailsClass() != null) {
            this.details = MAPPER.readValue(detailsJson, type.getDetailsClass());
        }
    }

    @PrePersist
    @PreUpdate
    private void serializeDetails() {
        this.detailsJson = details != null ? MAPPER.writeValueAsString(details) : null;
    }
}

