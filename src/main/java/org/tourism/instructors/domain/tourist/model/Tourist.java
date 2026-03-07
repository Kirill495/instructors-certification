package org.tourism.instructors.domain.tourist.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.tourism.instructors.domain.tourist.model.contactinfo.ContactInfoItem;

import java.time.LocalDate;
import java.util.List;

@Entity
@Getter
@Setter
@Table(name = "tourists", schema = "instructors_grades")
public class Tourist {
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "first_name", nullable = false, length = 150)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 150)
    private String lastName;

    @Column(name = "middle_name", length = 150)
    private String middleName;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender")
    private Gender gender;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "email")
    private String email;

    @Column(name = "certification_id")
    private String certificationId;

    @OneToMany(mappedBy = "tourist", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ContactInfoItem> contactInfo;

    public String getFullName() {
        return lastName + " " + firstName + " " + middleName;
    }
}
