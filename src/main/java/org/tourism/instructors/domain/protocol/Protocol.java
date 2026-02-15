package org.tourism.instructors.domain.protocol;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "protocols", schema = "instructors_grades")
public class Protocol {
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private int id;

    @Column(name = "number", length = 12)
    private String number;

    @Column(name = "date")
    private LocalDate date;

    @Column(name = "order_number", length = 12)
    private String order;

    @OneToMany(mappedBy = "protocol", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    List<ProtocolContent> protocolContents = new ArrayList<>();

}
