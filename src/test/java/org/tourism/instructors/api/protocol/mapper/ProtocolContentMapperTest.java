package org.tourism.instructors.api.protocol.mapper;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.tourism.instructors.api.protocol.dto.ProtocolContentDTO;
import org.tourism.instructors.api.protocol.dto.ProtocolContentFormRow;
import org.tourism.instructors.api.tourist.dto.TouristSummaryDTO;
import org.tourism.instructors.domain.catalog.model.Grade;
import org.tourism.instructors.domain.catalog.model.KindOfTourism;
import org.tourism.instructors.domain.protocol.ProtocolContent;
import org.tourism.instructors.domain.protocol.ProtocolContentPk;
import org.tourism.instructors.domain.tourist.model.Tourist;

@ExtendWith(SpringExtension.class)
@Import(ProtocolContentMapperImpl.class)
class ProtocolContentMapperTest {

    @Autowired
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    ProtocolContentMapper mapper;

    private Tourist tourist(int id, String lastName, String firstName, String middleName) {
        Tourist t = new Tourist();
        t.setId(id);
        t.setLastName(lastName);
        t.setFirstName(firstName);
        t.setMiddleName(middleName);
        return t;
    }

    private Grade grade(int id, String title) {
        Grade g = new Grade();
        g.setId(id);
        g.setTitle(title);
        return g;
    }

    private KindOfTourism kindOfTourism(int id, String title) {
        return new KindOfTourism(id, title, false);
    }

    private ProtocolContent content(
            int protocolId,
            int rowNum,
            Tourist tourist,
            Grade grade,
            KindOfTourism kindOfTourism,
            String certificationID,
            String club) {
        ProtocolContentPk pk = new ProtocolContentPk();
        pk.setProtocolId(protocolId);
        pk.setRowNum(rowNum);
        ProtocolContent c = new ProtocolContent();
        c.setId(pk);
        c.setTourist(tourist);
        c.setGrade(grade);
        c.setKindOfTourism(kindOfTourism);
        c.setCertificationID(certificationID);
        c.setClub(club);
        return c;
    }

    // ── toDTO ─────────────────────────────────────────────────────────────────

    @Test
    void toDTO_nullInput_returnsNull() {
        assertNull(mapper.toDTO(null));
    }

    @Test
    void toDTO_compositeKey_mappedToProtocolIdAndRowNum() {
        ProtocolContent c =
                content(
                        10,
                        3,
                        tourist(1, "Doe", "John", null),
                        grade(2, "Grade I"),
                        kindOfTourism(5, "Hiking"),
                        null,
                        null);
        ProtocolContentDTO dto = mapper.toDTO(c);
        assertEquals(10, dto.protocolId());
        assertEquals(3, dto.rowNum());
    }

    @Test
    void toDTO_certificationId_mappedFromCertificationID() {
        ProtocolContent c =
                content(
                        1,
                        1,
                        tourist(1, "Doe", "John", null),
                        grade(1, "Grade I"),
                        kindOfTourism(1, "Hiking"),
                        "CERT-001",
                        null);
        assertEquals("CERT-001", mapper.toDTO(c).certificationId());
    }

    @Test
    void toDTO_club_mappedDirectly() {
        ProtocolContent c =
                content(
                        1,
                        1,
                        tourist(1, "Doe", "John", null),
                        grade(1, "Grade I"),
                        kindOfTourism(1, "Hiking"),
                        null,
                        "Alpine Club");
        assertEquals("Alpine Club", mapper.toDTO(c).club());
    }

    @Test
    void toDTO_touristRef_hasIdAndTitle() {
        Tourist t = tourist(7, "Doe", "John", "Paul");
        ProtocolContent c = content(1, 1, t, grade(1, "G"), kindOfTourism(1, "H"), null, null);
        ProtocolContentDTO.Ref ref = mapper.toDTO(c).tourist();
        assertEquals(7, ref.id());
        assertEquals(t.getTitle(), ref.title());
    }

    @Test
    void toDTO_gradeRef_hasIdAndTitle() {
        ProtocolContent c =
                content(
                        1,
                        1,
                        tourist(1, "A", "B", null),
                        grade(3, "Grade III"),
                        kindOfTourism(1, "H"),
                        null,
                        null);
        ProtocolContentDTO.Ref ref = mapper.toDTO(c).grade();
        assertEquals(3, ref.id());
        assertEquals("Grade III", ref.title());
    }

    @Test
    void toDTO_kindOfTourismRef_hasIdAndTitle() {
        ProtocolContent c =
                content(
                        1,
                        1,
                        tourist(1, "A", "B", null),
                        grade(1, "G"),
                        kindOfTourism(4, "Climbing"),
                        null,
                        null);
        ProtocolContentDTO.Ref ref = mapper.toDTO(c).kindOfTourism();
        assertEquals(4, ref.id());
        assertEquals("Climbing", ref.title());
    }

    // ── toFormRow ─────────────────────────────────────────────────────────────

    @Test
    void toFormRow_nullInput_returnsNull() {
        assertNull(mapper.toFormRow(null));
    }

    @Test
    void toFormRow_compositeKey_mappedToProtocolIdAndRowNum() {
        ProtocolContent c =
                content(
                        10,
                        3,
                        tourist(1, "Doe", "John", null),
                        grade(1, "G"),
                        kindOfTourism(1, "H"),
                        null,
                        null);
        ProtocolContentFormRow row = mapper.toFormRow(c);
        assertEquals(10, row.getProtocolId());
        assertEquals(3, row.getRowNum());
    }

    @Test
    void toFormRow_touristIdAndTitle_mappedFromTourist() {
        Tourist t = tourist(7, "Doe", "John", "Paul");
        ProtocolContent c = content(1, 1, t, grade(1, "G"), kindOfTourism(1, "H"), null, null);
        ProtocolContentFormRow row = mapper.toFormRow(c);
        assertEquals(7, row.getTouristId());
        assertEquals(t.getTitle(), row.getTouristTitle());
    }

    @Test
    void toFormRow_kindOfTourismId_mappedFromKindOfTourism() {
        ProtocolContent c =
                content(
                        1,
                        1,
                        tourist(1, "A", "B", null),
                        grade(1, "G"),
                        kindOfTourism(4, "Climbing"),
                        null,
                        null);
        assertEquals(4, mapper.toFormRow(c).getKindOfTourismId());
    }

    @Test
    void toFormRow_gradeId_mappedFromGrade() {
        ProtocolContent c =
                content(
                        1,
                        1,
                        tourist(1, "A", "B", null),
                        grade(3, "Grade III"),
                        kindOfTourism(1, "H"),
                        null,
                        null);
        assertEquals(3, mapper.toFormRow(c).getGradeId());
    }

    @Test
    void toFormRow_certificationId_mappedFromCertificationID() {
        ProtocolContent c =
                content(
                        1,
                        1,
                        tourist(1, "A", "B", null),
                        grade(1, "G"),
                        kindOfTourism(1, "H"),
                        "CERT-099",
                        null);
        assertEquals("CERT-099", mapper.toFormRow(c).getCertificationId());
    }

    @Test
    void toFormRow_club_mappedDirectly() {
        ProtocolContent c =
                content(
                        1,
                        1,
                        tourist(1, "A", "B", null),
                        grade(1, "G"),
                        kindOfTourism(1, "H"),
                        null,
                        "Mountain Club");
        assertEquals("Mountain Club", mapper.toFormRow(c).getClub());
    }

    // ── toEntity ──────────────────────────────────────────────────────────────

    @Test
    void toEntity_nullInput_returnsNull() {
        assertNull(mapper.toEntity(null));
    }

    @Test
    void toEntity_protocolIdAndRowNum_mappedToCompositeKey() {
        ProtocolContentFormRow row =
                new ProtocolContentFormRow(10, 3, 1, "Doe John", 2, 5, "CERT-1", "Club");
        ProtocolContent entity = mapper.toEntity(row);
        assertEquals(10, entity.getId().getProtocolId());
        assertEquals(3, entity.getId().getRowNum());
    }

    @Test
    void toEntity_touristId_mappedToTouristEntity() {
        ProtocolContentFormRow row =
                new ProtocolContentFormRow(1, 1, 7, "Doe John", 2, 5, null, null);
        assertEquals(7, mapper.toEntity(row).getTourist().getId());
    }

    @Test
    void toEntity_kindOfTourismId_mappedToKindOfTourismEntity() {
        ProtocolContentFormRow row = new ProtocolContentFormRow(1, 1, 1, "Name", 4, 5, null, null);
        assertEquals(4, mapper.toEntity(row).getKindOfTourism().getId());
    }

    @Test
    void toEntity_gradeId_mappedToGradeEntity() {
        ProtocolContentFormRow row = new ProtocolContentFormRow(1, 1, 1, "Name", 2, 3, null, null);
        assertEquals(3, mapper.toEntity(row).getGrade().getId());
    }

    @Test
    void toEntity_certificationID_mappedFromCertificationId() {
        ProtocolContentFormRow row =
                new ProtocolContentFormRow(1, 1, 1, "Name", 2, 5, "CERT-007", null);
        assertEquals("CERT-007", mapper.toEntity(row).getCertificationID());
    }

    @Test
    void toEntity_protocol_isIgnored() {
        ProtocolContentFormRow row = new ProtocolContentFormRow(1, 1, 1, "Name", 2, 5, null, null);
        assertNull(mapper.toEntity(row).getProtocol());
    }

    // ── toTouristDTO ──────────────────────────────────────────────────────────

    @Test
    void toTouristDTO_nullInput_returnsNull() {
        assertNull(mapper.toTouristDTO(null));
    }

    @Test
    void toTouristDTO_touristFields_areMappedFromNestedTourist() {
        Tourist t = tourist(5, "Doe", "John", "Paul");
        ProtocolContent c = content(1, 1, t, grade(1, "G"), kindOfTourism(1, "H"), null, null);
        TouristSummaryDTO dto = mapper.toTouristDTO(c);
        assertEquals(5, dto.id());
        assertEquals("Doe", dto.lastName());
        assertEquals("John", dto.firstName());
        assertEquals("Paul", dto.middleName());
    }

    @Test
    void toTouristDTO_certificationId_isNull() {
        // certificationId on TouristSummaryDTO is not explicitly mapped —
        // tourist.certificationId is not referenced, so the field stays null
        Tourist t = tourist(1, "Doe", "John", null);
        t.setCertificationId("should-not-appear");
        ProtocolContent c = content(1, 1, t, grade(1, "G"), kindOfTourism(1, "H"), "CERT-X", null);
        assertNull(mapper.toTouristDTO(c).certificationId());
    }

    @Test
    void toTouristDTO_touristIdNull_idDefaultsToZero() {
        Tourist t = tourist(1, "Doe", "John", null);
        t.setId(null);
        ProtocolContent c = content(1, 1, t, grade(1, "G"), kindOfTourism(1, "H"), null, null);
        assertEquals(0, mapper.toTouristDTO(c).id());
    }

    // ── null nested objects ───────────────────────────────────────────────────

    @Test
    void toDTO_nullTourist_touristRefIsNull() {
        ProtocolContent c = content(1, 1, null, grade(1, "G"), kindOfTourism(1, "H"), null, null);
        assertNull(mapper.toDTO(c).tourist());
    }

    @Test
    void toDTO_nullGrade_gradeRefIsNull() {
        ProtocolContent c =
                content(1, 1, tourist(1, "A", "B", null), null, kindOfTourism(1, "H"), null, null);
        assertNull(mapper.toDTO(c).grade());
    }

    @Test
    void toDTO_nullKindOfTourism_kindOfTourismRefIsNull() {
        ProtocolContent c =
                content(1, 1, tourist(1, "A", "B", null), grade(1, "G"), null, null, null);
        assertNull(mapper.toDTO(c).kindOfTourism());
    }

    @Test
    void toFormRow_nullTourist_touristIdAndTitleAreNull() {
        ProtocolContent c = content(1, 1, null, grade(1, "G"), kindOfTourism(1, "H"), null, null);
        ProtocolContentFormRow row = mapper.toFormRow(c);
        assertNull(row.getTouristId());
        assertNull(row.getTouristTitle());
    }

    @Test
    void toFormRow_nullKindOfTourism_kindOfTourismIdIsNull() {
        ProtocolContent c =
                content(1, 1, tourist(1, "A", "B", null), grade(1, "G"), null, null, null);
        assertNull(mapper.toFormRow(c).getKindOfTourismId());
    }

    @Test
    void toFormRow_nullGrade_gradeIdIsNull() {
        ProtocolContent c =
                content(1, 1, tourist(1, "A", "B", null), null, kindOfTourism(1, "H"), null, null);
        assertNull(mapper.toFormRow(c).getGradeId());
    }

    @Test
    void toEntity_nullProtocolIdInFormRow_pkProtocolIdRemainsDefault() {
        ProtocolContentFormRow row =
                new ProtocolContentFormRow(null, 3, 1, "Name", 2, 5, null, null);
        assertEquals(0, mapper.toEntity(row).getId().getProtocolId());
    }

    @Test
    void toEntity_nullRowNumInFormRow_pkRowNumRemainsDefault() {
        ProtocolContentFormRow row =
                new ProtocolContentFormRow(1, null, 1, "Name", 2, 5, null, null);
        assertEquals(0, mapper.toEntity(row).getId().getRowNum());
    }
}
