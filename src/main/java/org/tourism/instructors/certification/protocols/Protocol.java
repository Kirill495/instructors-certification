package org.tourism.instructors.certification.protocols;

import jakarta.persistence.*;

import java.time.LocalDate;

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
}
