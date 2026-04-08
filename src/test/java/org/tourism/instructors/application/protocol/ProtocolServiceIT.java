package org.tourism.instructors.application.protocol;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.tourism.instructors.api.bot.BotInitializer;
import org.tourism.instructors.api.bot.TouristRegistrationBot;
import org.tourism.instructors.api.protocol.dto.ProtocolDTO;
import org.tourism.instructors.api.protocol.dto.ProtocolForListDTO;
import org.tourism.instructors.api.protocol.dto.ProtocolLightDTO;
import org.tourism.instructors.domain.catalog.model.Grade;
import org.tourism.instructors.domain.catalog.model.KindOfTourism;
import org.tourism.instructors.domain.catalog.repository.GradeRepository;
import org.tourism.instructors.domain.catalog.repository.KindOfTourismRepository;
import org.tourism.instructors.domain.pending.PendingTourist;
import org.tourism.instructors.domain.protocol.ProtocolStatus;
import org.tourism.instructors.domain.tourist.model.Tourist;
import org.tourism.instructors.domain.tourist.repository.TouristRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Testcontainers
class ProtocolServiceIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @MockitoBean
    TouristRegistrationBot bot;
    @MockitoBean
    BotInitializer botInitializer;

    @Autowired
    ProtocolService protocolService;
    @Autowired
    GradeRepository gradeRepository;
    @Autowired
    KindOfTourismRepository kindOfTourismRepository;

    @Autowired
    TouristRepository touristRepository;

    @Test
    void getProtocolsForList() {
        Page<ProtocolForListDTO> result = protocolService.getProtocolsForList(null, PageRequest.of(0, 10));
        assertEquals(2, result.getTotalElements());
    }

    @Test
    void getOneProtocolForList() {
        Page<ProtocolForListDTO> result = protocolService.getProtocolsForList(null, PageRequest.of(0, 1));
        assertEquals(1, result.getContent().size());
    }

    @Test
    void getProtocolForListWithSearchByName() {
        Page<ProtocolForListDTO> result = protocolService.getProtocolsForList("Толстой", PageRequest.of(0, 1));
        assertEquals(1, result.getContent().size());
        ProtocolForListDTO dto = result.getContent().getFirst();
        assertEquals(1, dto.getId());
        assertEquals(1, dto.getTourists().getFirst().id());
    }

    @Test
    void getProtocolForListWithSearchByNameWhenNoTouristFound() {
        Page<ProtocolForListDTO> result = protocolService.getProtocolsForList("_Толстой", PageRequest.of(0, 1));
        assertTrue(result.getContent().isEmpty());
    }

    @Test
    void getProtocolForListWithTooShortSearchString() {
        Page<ProtocolForListDTO> result = protocolService.getProtocolsForList("_", PageRequest.of(0, 10));
        assertEquals(2, result.getContent().size());
    }

    @Test
    @Transactional
    void addTouristToProtocolTest() {
        PendingTourist pt = new PendingTourist();
        Tourist tourist = touristRepository.findById(3).orElseThrow();
        KindOfTourism kindOfTourism = kindOfTourismRepository.findById(1).orElseThrow();
        Grade grade = gradeRepository.findById(1).orElseThrow();
        pt.setTourist(tourist);
        pt.setKindOfTourism(kindOfTourism);
        pt.setGrade(grade);
        protocolService.addTouristToProtocol(1, pt, 3);

        ProtocolDTO dto = protocolService.getProtocolById(1);
        assertEquals(3, dto.getContentRows().getLast().getTouristId());
    }

    @Test
    @Transactional
    void testSearchDraftsByNumberShouldReturnEmptyResult() {
        String protocolNumber = "100";
        List<ProtocolLightDTO> result = protocolService.searchDraftsByNumber(protocolNumber);
        assertTrue(result.isEmpty());
    }

    @Test
    @Transactional
    void testSearchDraftsByNumberShouldReturnOneProtocol() {
        String protocolNumber = "100";
        ProtocolDTO protocolDTO = protocolService.getProtocolById(1);
        protocolDTO.setStatus(ProtocolStatus.DRAFT);
        protocolService.saveProtocol(protocolDTO);

        List<ProtocolLightDTO> result = protocolService.searchDraftsByNumber(protocolNumber);
        assertFalse(result.isEmpty());
        assertEquals(1, result.getFirst().getId());
    }

    @Test
    @Transactional
    void testGetLastDraftsWhenAllProtocolsAreFinalized() {
        List<ProtocolLightDTO> result =  protocolService.getLastDrafts();
        assertTrue(result.isEmpty());
    }

    @Test
    @Transactional
    void testGetLastDrafts() {
        ProtocolDTO protocolDTO1 = protocolService.getProtocolById(1);
        protocolDTO1.setStatus(ProtocolStatus.DRAFT);
        protocolService.saveProtocol(protocolDTO1);

        ProtocolDTO protocolDTO2 = protocolService.getProtocolById(2);
        protocolDTO2.setStatus(ProtocolStatus.DRAFT);
        protocolService.saveProtocol(protocolDTO2);

        List<ProtocolLightDTO> result =  protocolService.getLastDrafts();
        assertEquals(2, result.size());
        assertEquals(2, result.getFirst().getId());
        assertEquals(1, result.getLast().getId());
    }

    @Test
    @Transactional
    void testCountProtocols() {
        assertEquals(2, protocolService.countProtocols());
    }

    @Test
    @Transactional
    void testCountProtocolsAfterDeleteOneProtocol() {
        protocolService.deleteProtocol(1);
        assertEquals(1, protocolService.countProtocols());
    }

    @Test
    @Transactional
    void testCountProtocolsAfterDeleteAllProtocols() {
        protocolService.deleteProtocol(1);
        protocolService.deleteProtocol(2);
        assertEquals(0, protocolService.countProtocols());
    }
}