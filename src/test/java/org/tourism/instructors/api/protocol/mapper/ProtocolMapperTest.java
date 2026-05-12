package org.tourism.instructors.api.protocol.mapper;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.tourism.instructors.api.protocol.dto.ProtocolContentDTO;
import org.tourism.instructors.api.protocol.dto.ProtocolContentFormRow;
import org.tourism.instructors.api.protocol.dto.ProtocolDTO;
import org.tourism.instructors.api.protocol.dto.ProtocolFormDTO;
import org.tourism.instructors.api.protocol.dto.ProtocolForListDTO;
import org.tourism.instructors.api.protocol.dto.ProtocolLiteDTO;
import org.tourism.instructors.api.tourist.dto.TouristSummaryDTO;
import org.tourism.instructors.domain.protocol.Protocol;
import org.tourism.instructors.domain.protocol.ProtocolContent;
import org.tourism.instructors.domain.protocol.ProtocolStatus;
import org.tourism.instructors.domain.protocol.repository.ProtocolRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@Import(ProtocolMapperImpl.class)
class ProtocolMapperTest {

    @Autowired
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    ProtocolMapper mapper;

    @MockitoBean
    ProtocolContentMapper protocolContentMapper;

    private Protocol protocol(Integer id, String number, LocalDate date,
                               String order, ProtocolStatus status,
                               List<ProtocolContent> contents) {
        Protocol p = new Protocol();
        p.setId(id);
        p.setNumber(number);
        p.setDate(date);
        p.setOrder(order);
        p.setStatus(status);
        p.setProtocolContents(contents != null ? contents : new ArrayList<>());
        return p;
    }

    private ProtocolRepository.ProtocolProjection projection(Integer id, String number, LocalDate date) {
        return new ProtocolRepository.ProtocolProjection() {
            @Override public Integer getId() { return id; }
            @Override public String getNumber() { return number; }
            @Override public LocalDate getDate() { return date; }
        };
    }

    // ── toDTO ─────────────────────────────────────────────────────────────────

    @Nested
    class ToDTO {

        @Test
        void nullInput_returnsNull() {
            assertNull(mapper.toDTO(null));
        }

        @Test
        void scalarFields_areMapped() {
            Protocol p = protocol(42, "П-001", LocalDate.of(2024, 3, 15), "order-1", ProtocolStatus.FINALIZED, null);
            ProtocolDTO dto = mapper.toDTO(p);
            assertEquals(42, dto.id());
            assertEquals("П-001", dto.number());
            assertEquals(LocalDate.of(2024, 3, 15), dto.date());
            assertEquals("order-1", dto.order());
            assertEquals(ProtocolStatus.FINALIZED, dto.status());
        }

        @Test
        void contentRows_delegatedToProtocolContentMapper() {
            ProtocolContent content = new ProtocolContent();
            ProtocolContentDTO stub = new ProtocolContentDTO(1, 1, null, null, null, null, null);
            when(protocolContentMapper.toDTO(content)).thenReturn(stub);

            Protocol p = protocol(1, "П-001", LocalDate.now(), "o", ProtocolStatus.DRAFT, List.of(content));
            ProtocolDTO dto = mapper.toDTO(p);

            assertEquals(1, dto.contentRows().size());
            assertSame(stub, dto.contentRows().getFirst());
            verify(protocolContentMapper).toDTO(content);
        }

        @Test
        void nullProtocolContents_contentRowsIsNull() {
            Protocol p = protocol(1, "П-001", LocalDate.now(), "o", ProtocolStatus.DRAFT, null);
            p.setProtocolContents(null);
            assertNull(mapper.toDTO(p).contentRows());
        }
    }

    // ── toFormDTO ─────────────────────────────────────────────────────────────

    @Nested
    class ToFormDTO {

        @Test
        void nullInput_returnsNull() {
            assertNull(mapper.toFormDTO(null));
        }

        @Test
        void scalarFields_areMapped() {
            Protocol p = protocol(5, "П-002", LocalDate.of(2023, 1, 1), "ord", ProtocolStatus.DRAFT, null);
            ProtocolFormDTO dto = mapper.toFormDTO(p);
            assertEquals(5, dto.getId());
            assertEquals("П-002", dto.getNumber());
            assertEquals(LocalDate.of(2023, 1, 1), dto.getDate());
            assertEquals("ord", dto.getOrder());
            assertEquals(ProtocolStatus.DRAFT, dto.getStatus());
        }

        @Test
        void contentRows_delegatedToProtocolContentMapper() {
            ProtocolContent content = new ProtocolContent();
            ProtocolContentFormRow stub = new ProtocolContentFormRow(1, 1, 1, "Tourist", 1, 1, null, null);
            when(protocolContentMapper.toFormRow(content)).thenReturn(stub);

            Protocol p = protocol(1, "П-001", LocalDate.now(), "o", ProtocolStatus.DRAFT, List.of(content));
            ProtocolFormDTO dto = mapper.toFormDTO(p);

            assertEquals(1, dto.getContentRows().size());
            assertSame(stub, dto.getContentRows().getFirst());
            verify(protocolContentMapper).toFormRow(content);
        }

        @Test
        void nullProtocolContents_contentRowsIsNull() {
            Protocol p = protocol(1, "П-001", LocalDate.now(), "o", ProtocolStatus.DRAFT, null);
            p.setProtocolContents(null);
            assertNull(mapper.toFormDTO(p).getContentRows());
        }
    }

    // ── toEntity ──────────────────────────────────────────────────────────────

    @Nested
    class ToEntity {

        @Test
        void nullInput_returnsNull() {
            assertNull(mapper.toEntity(null));
        }

        @Test
        void scalarFields_areMapped() {
            ProtocolFormDTO dto = new ProtocolFormDTO(7, "П-003", LocalDate.of(2022, 6, 1), "ord-2", ProtocolStatus.FINALIZED, null);
            Protocol entity = mapper.toEntity(dto);
            assertEquals(7, entity.getId());
            assertEquals("П-003", entity.getNumber());
            assertEquals(LocalDate.of(2022, 6, 1), entity.getDate());
            assertEquals("ord-2", entity.getOrder());
            assertEquals(ProtocolStatus.FINALIZED, entity.getStatus());
        }

        @Test
        void protocolContents_delegatedToProtocolContentMapper() {
            ProtocolContentFormRow row = new ProtocolContentFormRow(1, 1, 1, "Tourist", 1, 1, null, null);
            ProtocolContent stub = new ProtocolContent();
            when(protocolContentMapper.toEntity(row)).thenReturn(stub);

            ProtocolFormDTO dto = new ProtocolFormDTO(1, "П-001", LocalDate.now(), "o", ProtocolStatus.DRAFT, List.of(row));
            Protocol entity = mapper.toEntity(dto);

            assertEquals(1, entity.getProtocolContents().size());
            assertSame(stub, entity.getProtocolContents().getFirst());
            verify(protocolContentMapper).toEntity(row);
        }

        @Test
        void nullContentRows_protocolContentsIsNull() {
            ProtocolFormDTO dto = new ProtocolFormDTO(1, "П-001", LocalDate.now(), "o", ProtocolStatus.DRAFT, null);
            assertNull(mapper.toEntity(dto).getProtocolContents());
        }

        @Test
        void afterMapping_eachContentHasProtocolRefSet() {
            ProtocolContentFormRow row = new ProtocolContentFormRow(1, 1, 1, "Tourist", 1, 1, null, null);
            ProtocolContent content = new ProtocolContent();
            when(protocolContentMapper.toEntity(row)).thenReturn(content);

            ProtocolFormDTO dto = new ProtocolFormDTO(1, "П-001", LocalDate.now(), "o", ProtocolStatus.DRAFT, List.of(row));
            Protocol result = mapper.toEntity(dto);

            assertSame(result, content.getProtocol());
        }

        @Test
        void afterMapping_nullContentsList_doesNotThrow() {
            ProtocolFormDTO dto = new ProtocolFormDTO(1, "П-001", LocalDate.now(), "o", ProtocolStatus.DRAFT, null);
            assertDoesNotThrow(() -> mapper.toEntity(dto));
        }
    }

    // ── toProtocolForListDTO ──────────────────────────────────────────────────

    @Nested
    class ToProtocolForListDTO {

        @Test
        void nullInput_returnsNull() {
            assertNull(mapper.toProtocolForListDTO(null));
        }

        @Test
        void scalarFields_areMapped() {
            Protocol p = protocol(3, "П-010", LocalDate.of(2024, 7, 4), "ord-x", ProtocolStatus.DRAFT, null);
            ProtocolForListDTO dto = mapper.toProtocolForListDTO(p);
            assertEquals(3, dto.id());
            assertEquals("П-010", dto.number());
            assertEquals(LocalDate.of(2024, 7, 4), dto.date());
            assertEquals("ord-x", dto.order());
            assertEquals(ProtocolStatus.DRAFT, dto.status());
        }

        @Test
        void tourists_delegatedToProtocolContentMapper() {
            ProtocolContent content = new ProtocolContent();
            TouristSummaryDTO stub = new TouristSummaryDTO(1, "Ivan", "Petrov", null, null);
            when(protocolContentMapper.toTouristDTO(content)).thenReturn(stub);

            Protocol p = protocol(1, "П-001", LocalDate.now(), "o", ProtocolStatus.DRAFT, List.of(content));
            ProtocolForListDTO dto = mapper.toProtocolForListDTO(p);

            assertEquals(1, dto.tourists().size());
            assertSame(stub, dto.tourists().getFirst());
        }

        @Test
        void nullProtocolId_idDefaultsToZero() {
            Protocol p = protocol(null, "П-001", LocalDate.now(), "o", ProtocolStatus.DRAFT, null);
            assertEquals(0, mapper.toProtocolForListDTO(p).id());
        }

        @Test
        void nullProtocolContents_touristsDefaultsToEmptyList() {
            Protocol p = protocol(1, "П-001", LocalDate.now(), "o", ProtocolStatus.DRAFT, null);
            p.setProtocolContents(null);
            assertNotNull(mapper.toProtocolForListDTO(p).tourists());
            assertTrue(mapper.toProtocolForListDTO(p).tourists().isEmpty());
        }
    }

    // ── toLightDTO ────────────────────────────────────────────────────────────

    @Nested
    class ToLightDTO {

        @Test
        void nullInput_returnsNull() {
            assertNull(mapper.toLightDTO(null));
        }

        @Test
        void fieldsFromProjection_areMapped() {
            LocalDate date = LocalDate.of(2024, 5, 20);
            ProtocolLiteDTO dto = mapper.toLightDTO(projection(8, "П-100", date));
            assertEquals(8, dto.id());
            assertEquals("П-100", dto.number());
            assertEquals(date, dto.date());
        }

        @Test
        void nullProjectionId_idDefaultsToZero() {
            ProtocolLiteDTO dto = mapper.toLightDTO(projection(null, "П-100", LocalDate.now()));
            assertEquals(0, dto.id());
        }
    }
}