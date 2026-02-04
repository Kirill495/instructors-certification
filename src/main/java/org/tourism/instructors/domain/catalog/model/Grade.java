package org.tourism.instructors.domain.catalog.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "grades", schema = "instructors_grades")
@Getter
@Setter
public class Grade {
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "title")
    private String title;

    @Column(name = "expires_in")
    private int expiresInYears;
}
