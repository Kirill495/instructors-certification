package org.tourism.instructors.api.pending.mapper;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.tourism.instructors.api.catalog.dto.GradeDTO;
import org.tourism.instructors.api.catalog.dto.KindOfTourismDTO;
import org.tourism.instructors.domain.pending.ConversationState;
import org.tourism.instructors.domain.pending.PendingTourist;
import org.tourism.instructors.domain.tourist.model.Gender;

@ExtendWith(SpringExtension.class)
@Import(PendingTouristMapperImpl.class)
class PendingTouristMapperTest {

    @Autowired
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    PendingTouristMapper mapper;

    ConversationState state;

    @BeforeEach
    void setUp() {
        state = new ConversationState(100L, "tg_user");
        state.setFirstName("John");
        state.setLastName("Doe");
        state.setMiddleName("Paul");
        state.setDateOfBirth("15.06.1990");
        state.setEmail("john@example.com");
        state.setPhoneNumber("+7-999-000-00-00");
        state.setCertificationId("CERT-42");
        state.setGender(Gender.MALE);
        state.setKindOfTourism(new KindOfTourismDTO(3, "Hiking", false));
        state.setGrade(new GradeDTO(2, "Grade I", false, 5));
    }

    @Test
    void toEntity_nullInput_returnsNull() {
        assertNull(mapper.toEntity(null));
    }

    @Test
    void toEntity_scalarFields_areMapped() {
        PendingTourist entity = mapper.toEntity(state);
        assertEquals(100L, entity.getChatId());
        assertEquals("tg_user", entity.getTgUsername());
        assertEquals("John", entity.getFirstName());
        assertEquals("Doe", entity.getLastName());
        assertEquals("Paul", entity.getMiddleName());
        assertEquals("john@example.com", entity.getEmail());
        assertEquals("+7-999-000-00-00", entity.getPhoneNumber());
        assertEquals("CERT-42", entity.getCertificationId());
        assertEquals(Gender.MALE, entity.getGender());
    }

    @Test
    void toEntity_dateOfBirth_parsedFromDdMmYyyyString() {
        PendingTourist entity = mapper.toEntity(state);
        assertEquals(LocalDate.of(1990, 6, 15), entity.getDateOfBirth());
    }

    @Test
    void toEntity_dateOfBirth_nullWhenSourceIsNull() {
        state.setDateOfBirth(null);
        assertNull(mapper.toEntity(state).getDateOfBirth());
    }

    @Test
    void toEntity_dateOfBirth_throwsOnInvalidFormat() {
        state.setDateOfBirth("1990-06-15"); // wrong format — expects dd.MM.yyyy
        assertThrows(DateTimeParseException.class, () -> mapper.toEntity(state));
    }

    @Test
    void toEntity_kindOfTourism_isMappedFromDTO() {
        PendingTourist entity = mapper.toEntity(state);
        assertNotNull(entity.getKindOfTourism());
        assertEquals(3, entity.getKindOfTourism().getId());
        assertEquals("Hiking", entity.getKindOfTourism().getTitle());
    }

    @Test
    void toEntity_grade_isMappedFromDTO() {
        PendingTourist entity = mapper.toEntity(state);
        assertNotNull(entity.getGrade());
        assertEquals(2, entity.getGrade().getId());
        assertEquals("Grade I", entity.getGrade().getTitle());
        assertEquals(5, entity.getGrade().getExpiresInYears());
    }

    @Test
    void toEntity_id_isNull() {
        // ConversationState has no id — PendingTourist.id must stay null (DB-generated)
        assertNull(mapper.toEntity(state).getId());
    }

    @Test
    void toEntity_tourist_isNull() {
        // Tourist link is resolved after registration, not during conversation mapping
        assertNull(mapper.toEntity(state).getTourist());
    }

    @Test
    void toEntity_status_keepsDefaultValue() {
        assertEquals("PENDING", mapper.toEntity(state).getStatus());
    }

    @Test
    void toEntity_createdAt_isNotNull() {
        assertNotNull(mapper.toEntity(state).getCreatedAt());
    }
}
