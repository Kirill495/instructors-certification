package org.tourism.instructors.certification.common;

import jakarta.persistence.*;

@Entity
@Table(name = "kinds_of_tourism", schema = "instructors_grades")
public class KindOfTourism {
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "title")
    private String title;
}
