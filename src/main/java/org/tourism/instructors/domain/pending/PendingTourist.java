package org.tourism.instructors.domain.pending;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.tourism.instructors.domain.catalog.model.Grade;
import org.tourism.instructors.domain.catalog.model.KindOfTourism;
import org.tourism.instructors.domain.tourist.model.Gender;
import org.tourism.instructors.domain.tourist.model.Tourist;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "pending_tourists", schema = "instructors_grades")
public class PendingTourist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "chat_id", nullable = false)
    private Long chatId;

    @Column(name = "tg_username")
    private String tgUsername;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "middle_name")
    private String middleName;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "email")
    private String email;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "gender", nullable = false)
    @Enumerated(value = EnumType.STRING)
    private Gender gender;

    @OneToOne
    @JoinColumn(name = "kind_of_tourism", nullable = false)
    private KindOfTourism kindOfTourism;

    @OneToOne(targetEntity = Grade.class)
    @JoinColumn(name = "grade", nullable = false)
    private Grade grade;

    @OneToOne
    @JoinColumn(name = "tourist")
    private Tourist tourist;

    @Column(name = "certification_id")
    private String certificationId;

    @Column(name = "status", nullable = false)
    private String status = "PENDING";

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public String getFullName() {
        return lastName + " " + firstName + (middleName != null ? " " + middleName : "");
    }
}