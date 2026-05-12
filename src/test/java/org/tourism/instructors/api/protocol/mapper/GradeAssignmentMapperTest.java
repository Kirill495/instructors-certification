package org.tourism.instructors.api.protocol.mapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.tourism.instructors.api.catalog.mapper.GradeMapperImpl;
import org.tourism.instructors.api.catalog.mapper.KindOfTourismMapperImpl;
import org.tourism.instructors.api.tourist.dto.TouristDTO;
import org.tourism.instructors.domain.catalog.model.Grade;
import org.tourism.instructors.domain.catalog.model.KindOfTourism;
import org.tourism.instructors.domain.protocol.repository.ProtocolRepository;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@Import({
        GradeAssignmentMapperImpl.class,
        GradeMapperImpl.class,
        KindOfTourismMapperImpl.class
})
class GradeAssignmentMapperTest {

    @Autowired
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    GradeAssignmentMapper mapper;

    private Grade grade(int expiresInYears) {
        Grade g = new Grade();
        g.setId(1);
        g.setTitle("Grade I");
        g.setExpiresInYears(expiresInYears);
        return g;
    }

    private KindOfTourism kindOfTourism() {
        return new KindOfTourism(1, "Hiking", false);
    }

    private ProtocolRepository.GradeAssignmentProjection projection(
            int protocolId, LocalDate protocolDate, Grade grade, KindOfTourism kindOfTourism) {
        return new ProtocolRepository.GradeAssignmentProjection() {
            @Override public Integer getProtocolId() { return protocolId; }
            @Override public LocalDate getProtocolDate() { return protocolDate; }
            @Override public Integer getTouristId() { return 7; }
            @Override public Grade getGrade() { return grade; }
            @Override public KindOfTourism getKindOfTourism() { return kindOfTourism; }
        };
    }

    @Test
    void toDTO_nullInput_returnsNull() {
        assertNull(mapper.toDTO(null));
    }

    @Test
    void toDTO_date_equalsProtocolDate() {
        LocalDate protocolDate = LocalDate.of(2020, 6, 15);
        TouristDTO.AssignmentDTO dto = mapper.toDTO(projection(1, protocolDate, grade(3), kindOfTourism()));
        assertEquals(protocolDate, dto.date());
    }

    @Test
    void toDTO_validThrough_equalsProtocolDatePlusExpiresInYears() {
        LocalDate protocolDate = LocalDate.of(2020, 6, 15);
        TouristDTO.AssignmentDTO dto = mapper.toDTO(projection(1, protocolDate, grade(3), kindOfTourism()));
        assertEquals(LocalDate.of(2023, 6, 15), dto.validThrough());
    }

    @Test
    void toDTO_valid_trueWhenValidThroughIsInFuture() {
        LocalDate protocolDate = LocalDate.now().minusYears(1);
        TouristDTO.AssignmentDTO dto = mapper.toDTO(projection(1, protocolDate, grade(5), kindOfTourism()));
        assertTrue(dto.valid());
    }

    @Test
    void toDTO_valid_falseWhenValidThroughIsInPast() {
        LocalDate protocolDate = LocalDate.of(2010, 1, 1);
        TouristDTO.AssignmentDTO dto = mapper.toDTO(projection(1, protocolDate, grade(2), kindOfTourism()));
        assertFalse(dto.valid());
    }

    @Test
    void toDTO_valid_falseWhenValidThroughIsToday() {
        // isAfter is strict — a certificate expiring today is no longer valid
        LocalDate today = LocalDate.now();
        TouristDTO.AssignmentDTO dto = mapper.toDTO(projection(1, today.minusYears(3), grade(3), kindOfTourism()));
        assertEquals(today, dto.validThrough());
        assertFalse(dto.valid());
    }

    @Test
    void toDTO_protocolId_isMapped() {
        TouristDTO.AssignmentDTO dto = mapper.toDTO(projection(42, LocalDate.of(2022, 1, 1), grade(1), kindOfTourism()));
        assertEquals(42, dto.protocolId());
    }

    @Test
    void toDTO_grade_isMappedViaGradeMapper() {
        Grade g = grade(2);
        TouristDTO.AssignmentDTO dto = mapper.toDTO(projection(1, LocalDate.of(2022, 1, 1), g, kindOfTourism()));
        assertNotNull(dto.grade());
        assertEquals(1, dto.grade().id());
        assertEquals("Grade I", dto.grade().title());
        assertEquals(2, dto.grade().expiresInYears());
    }

    @Test
    void toDTO_kindOfTourism_isMappedViaKindOfTourismMapper() {
        TouristDTO.AssignmentDTO dto = mapper.toDTO(projection(1, LocalDate.of(2022, 1, 1), grade(1), kindOfTourism()));
        assertNotNull(dto.kindOfTourism());
        assertEquals(1, dto.kindOfTourism().getId());
        assertEquals("Hiking", dto.kindOfTourism().getTitle());
    }
}