package org.tourism.instructors.api.tourist.mapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.tourism.instructors.api.catalog.mapper.GradeMapperImpl;
import org.tourism.instructors.api.catalog.mapper.KindOfTourismMapperImpl;
import org.tourism.instructors.api.protocol.mapper.GradeAssignmentMapperImpl;
import org.tourism.instructors.api.tourist.dto.ContactInfoItemDTO;
import org.tourism.instructors.api.tourist.dto.TouristDTO;
import org.tourism.instructors.api.tourist.dto.TouristSummaryDTO;
import org.tourism.instructors.domain.catalog.model.Grade;
import org.tourism.instructors.domain.catalog.model.KindOfTourism;
import org.tourism.instructors.domain.pending.PendingTourist;
import org.tourism.instructors.domain.protocol.repository.ProtocolRepository;
import org.tourism.instructors.domain.tourist.model.Tourist;
import org.tourism.instructors.domain.tourist.model.contactinfo.ContactInfoItem;
import org.tourism.instructors.domain.tourist.model.contactinfo.ContactInfoType;
import org.tourism.instructors.domain.tourist.model.contactinfo.TelegramDetails;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@ExtendWith(SpringExtension.class)
@Import({
        TouristMapperImpl.class,
        GradeAssignmentMapperImpl.class,
        ContactInfoMapperImpl.class,
        GradeMapperImpl.class,
        KindOfTourismMapperImpl.class
})
class TouristMapperTest {

    @Autowired
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    TouristMapper touristMapper;

    Tourist tourist;
    TelegramDetails tgNickName;
    static String FIRST_NAME = "first_name";
    static String LAST_NAME = "last_name";
    static String MIDDLE_NAME = "middle_name";
    static String CERTIFICATION_ID = "certification_id";
    Grade grade = new Grade();
    KindOfTourism kindOfTourism = new KindOfTourism(1, "KindOfTourism", false);
    ContactInfoItem contactInfoItem = new ContactInfoItem();

    @BeforeEach
    void setUp() {

        grade.setId(1);
        grade.setTitle("grade");

        tourist = new Tourist();
        tourist.setId(1);
        tourist.setFirstName(FIRST_NAME);
        tourist.setLastName(LAST_NAME);
        tourist.setMiddleName(MIDDLE_NAME);
        tourist.setCertificationId(CERTIFICATION_ID);

        contactInfoItem.setId(1);
        contactInfoItem.setTourist(tourist);
        contactInfoItem.setType(ContactInfoType.TELEGRAM);
        contactInfoItem.setValue("tg_id");
        tgNickName = new TelegramDetails(1, "tg_nick_name");
        contactInfoItem.setDetails(tgNickName);

        tourist.getContactInfo().add(contactInfoItem);
    }

    @Test
    void testToDTO() {
        TouristDTO dto = touristMapper.toDTO(tourist);
        assertEquals(1, dto.getContactInfo().size());
        ContactInfoItemDTO item = dto.getContactInfo().getFirst();
        assertEquals(1, item.getId());
        assertEquals(1, item.getTouristId());
        assertEquals(ContactInfoType.TELEGRAM, item.getType());
        assertEquals("tg_id", item.getValue());
    }

    @Test
    void testToDTOWithAssignment() {
        TouristDTO dto = touristMapper.toDTO(tourist, getAssignments());
        assertEquals(1, dto.getContactInfo().size());
        ContactInfoItemDTO item = dto.getContactInfo().getFirst();
        assertEquals(1, item.getId());
        assertEquals(1, item.getTouristId());
        assertEquals(ContactInfoType.TELEGRAM, item.getType());
        assertEquals("tg_id", item.getValue());
        assertEquals(1, dto.getAssignments().size());
        TouristDTO.AssignmentDTO assignmentDTO = dto.getAssignments().getFirst();
        assertEquals(1, assignmentDTO.kindOfTourism().getId());
        assertEquals(1, assignmentDTO.grade().id());
    }

    @Test
    void testToDTOWithAssignmentWhenNull() {
        TouristDTO result = touristMapper.toDTOWithAssignments(null, null);
        assertNull(result);
    }

    @Test
    void testToDTOWithAssignmentAndContactInfo() {
        TouristDTO dto = touristMapper.toDTO(tourist, getAssignments(), List.of(contactInfoItem));
        assertEquals(1, dto.getContactInfo().size());
        ContactInfoItemDTO item = dto.getContactInfo().getFirst();
        assertEquals(1, item.getId());
        assertEquals(1, item.getTouristId());
        assertEquals(ContactInfoType.TELEGRAM, item.getType());
        assertEquals("tg_id", item.getValue());
        assertEquals(1, dto.getAssignments().size());
        TouristDTO.AssignmentDTO assignmentDTO = dto.getAssignments().getFirst();
        assertEquals(1, assignmentDTO.kindOfTourism().getId());
        assertEquals(1, assignmentDTO.grade().id());
    }

    private List<ProtocolRepository.GradeAssignmentProjection> getAssignments() {
        ProtocolRepository.GradeAssignmentProjection assignment = new ProtocolRepository.GradeAssignmentProjection() {
            @Override
            public Integer getProtocolId() {
                return 1;
            }

            @Override
            public LocalDate getProtocolDate() {
                return LocalDate.of(2020, 1, 1);
            }

            @Override
            public Integer getTouristId() {
                return 1;
            }

            @Override
            public Grade getGrade() {
                return grade;
            }

            @Override
            public KindOfTourism getKindOfTourism() {
                return kindOfTourism;
            }
        };

        return List.of(assignment);
    }


    @Test
    void testPendingTouristToDTO() {
        PendingTourist pendingTourist = new PendingTourist();
        String phone = "phone";
        pendingTourist.setPhoneNumber(phone);
        String email = "email";
        pendingTourist.setEmail(email);
        pendingTourist.setId(1);
        TouristDTO dto = touristMapper.toDTO(pendingTourist);
        assertNull(dto.getId());
        assertEquals(phone, dto.getPhoneNumber());
        assertEquals(email, dto.getEmail());
    }

    @Test
    void testPendingTouristToDTOWhenNull() {
        TouristDTO dto = touristMapper.toDTO((PendingTourist) null);
        assertNull(dto);
    }

    @Test
    void testPendingTouristToDTOWhenNull_1() {
        TouristDTO dto = touristMapper.toDTO((Tourist) null);
        assertNull(dto);
    }


    @Test
    void testPendingTouristToDTOWhenNull_2() {
        TouristDTO dto = touristMapper.toDTO(null, List.of());
        assertNull(dto);
    }

    @Test
    void testPendingTouristToDTOWhenNull_3() {
        TouristDTO dto = touristMapper.toDTO(null, List.of(), List.of());
        assertNull(dto);
    }

    @Test
    void testPendingTouristToDTOWhenNull_4() {
        TouristDTO dto = touristMapper.toDTO(null, null, null);
        assertNull(dto);
    }

    @Test
    void testToLightDTO() {
        TouristSummaryDTO dto = touristMapper.toLightDTO(tourist);
        assertEquals(1, dto.id());
        assertEquals(FIRST_NAME, dto.firstName());
        assertEquals(LAST_NAME, dto.lastName());
        assertEquals(MIDDLE_NAME, dto.middleName());
        assertEquals(CERTIFICATION_ID, dto.certificationId());
        assertEquals(String.join(" ", LAST_NAME, FIRST_NAME, MIDDLE_NAME), dto.getFullName());
    }

    @Test
    void testToLightDTOWhenNull() {
        TouristSummaryDTO dto = touristMapper.toLightDTO(null);
        assertNull(dto);
    }

    @Test
    void testToEntity() {
        TouristDTO dto = new TouristDTO();
        dto.setId(1);
        ContactInfoItemDTO ciDTO = new ContactInfoItemDTO(1, 1, ContactInfoType.TELEGRAM, "11", new TelegramDetails(1, "@tgNickName"));
        dto.setContactInfo(List.of(ciDTO));
        Tourist result = touristMapper.toEntity(dto);
        assertEquals(1, result.getId());
        assertEquals(ContactInfoType.TELEGRAM, result.getContactInfo().getFirst().getType());
    }

    @Test
    void testToEntityWhenContactInfoIsNull() {
        TouristDTO dto = new TouristDTO();
        dto.setId(1);
        Tourist result = touristMapper.toEntity(dto);
        assertEquals(1, result.getId());
        assertNull(result.getContactInfo());
    }

    @Test
    void testToEntityWhenNull() {
        Tourist result = touristMapper.toEntity(null);
        assertNull(result);
    }

    @Test
    void testToDTOWithAssignmentsAndContactInfoWhenAllIsNull() {
        TouristDTO dto = touristMapper.toDTOWithAssignmentsAndContactInfo(null, null, null);
        assertNull(dto);
    }
}