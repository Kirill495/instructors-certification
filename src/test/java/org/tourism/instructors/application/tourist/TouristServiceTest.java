package org.tourism.instructors.application.tourist;

import org.apache.logging.log4j.util.Strings;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.tourism.instructors.api.tourist.dto.ContactInfoItemDTO;
import org.tourism.instructors.api.tourist.dto.TouristDTO;
import org.tourism.instructors.api.tourist.dto.TouristLightDTO;
import org.tourism.instructors.api.tourist.mapper.TouristMapper;
import org.tourism.instructors.application.tourist.exception.TouristCannotBeDeletedException;
import org.tourism.instructors.application.tourist.exception.TouristNotFoundException;
import org.tourism.instructors.application.tourist.impl.TouristServiceImpl;
import org.tourism.instructors.domain.protocol.repository.ProtocolRepository;
import org.tourism.instructors.domain.tourist.model.Tourist;
import org.tourism.instructors.domain.tourist.model.contactinfo.ContactInfoItem;
import org.tourism.instructors.domain.tourist.model.contactinfo.ContactInfoType;
import org.tourism.instructors.domain.tourist.repository.ContactInfoRepository;
import org.tourism.instructors.domain.tourist.repository.TouristRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TouristServiceTest {

    @Mock
    TouristRepository touristRepository;
    @Mock
    ProtocolRepository protocolRepository;
    @Mock
    TouristMapper touristMapper;
    @Mock
    ContactInfoRepository contactInfoRepository;
    @Mock
    ProtocolRepository.GradeAssignmentProjection projection;

    @InjectMocks
    TouristServiceImpl touristService;

    @Nested
    class GetAllTourists {
        @Test
        void getAllTouristsWhenNoTouristExists() {
            Pageable pageable = PageRequest.of(0, 1);
            Page<Tourist> touristPage = new PageImpl<>(List.of(), pageable, 0L);
            when(touristRepository.findAll(pageable)).thenReturn(touristPage);
            when(protocolRepository.getAssignments(List.of())).thenReturn(List.of());

            var result = touristService.getAllTourists(pageable);

            verify(touristRepository).findAll(pageable);
            assertTrue(result.isEmpty());

        }

        @Test
        void getAllTouristsWhenOneTouristExists() {
            Tourist tourist = new Tourist();
            tourist.setId(1);

            Pageable pageable = PageRequest.of(0, 1);
            Page<Tourist> touristPage = new PageImpl<>(List.of(tourist), pageable, 1L);

            when(touristRepository.findAll(pageable)).thenReturn(touristPage);
            when(projection.getTouristId()).thenReturn(1);
            when(protocolRepository.getAssignments(anyList())).thenReturn(List.of(projection));
            TouristDTO dto = new TouristDTO();
            when(touristMapper.toDTO(tourist, List.of(projection))).thenReturn(dto);

            var result = touristService.getAllTourists(pageable);

            assertEquals(1L, result.getTotalElements());
            assertEquals(1, result.getContent().size());
            assertEquals(dto, result.getContent().getFirst());

        }
    }

    @Nested
    class DeleteTourist {
        @Test
        void deleteTouristWithGradeAssignmentsShouldFail() {
            when(protocolRepository.getAssignments(anyList())).thenReturn(List.of(projection));
            assertThrows(TouristCannotBeDeletedException.class, () -> touristService.delete(1));
        }

        @Test
        void deleteTouristWithoutGradeAssignments() {
            when(protocolRepository.getAssignments(anyList())).thenReturn(List.of());
            touristService.delete(1);
            verify(touristRepository).deleteById(1);
        }
    }

    @Nested
    class SearchTourists {

        @Test
        void searchTouristsWithLastName() {
            String searchString = "surname";
            Pageable pageable = PageRequest.of(0, 10);

            Tourist tourist = new Tourist();
            tourist.setId(1);
            when(touristRepository.searchByLastNameStartingWithIgnoreCase(searchString)).thenReturn(List.of(tourist));
            TouristDTO touristDTO = new TouristDTO();
            touristDTO.setId(1);
            when(touristMapper.toDTO(eq(tourist), anyList())).thenReturn(touristDTO);

            Page<TouristDTO> result = touristService.searchTourists(searchString, pageable);
            assertEquals(touristDTO, result.getContent().getFirst());
        }

        @Test
        void searchTouristsWithID() {
            String searchString = "1234";
            Pageable pageable = PageRequest.of(0, 10);

            Tourist tourist = new Tourist();
            tourist.setId(1);
            when(touristRepository.searchByCertificationId(searchString)).thenReturn(List.of(tourist));
            TouristDTO touristDTO = new TouristDTO();
            touristDTO.setId(1);
            when(touristMapper.toDTO(eq(tourist), anyList())).thenReturn(touristDTO);

            Page<TouristDTO> result = touristService.searchTourists(searchString, pageable);
            assertEquals(touristDTO, result.getContent().getFirst());
        }
    }

    @Nested
    class CountTourists {
        @Test
        void countTourists() {
            when(touristRepository.count()).thenReturn(10L);
            int result = touristService.countTourists();
            assertEquals(10, result);
        }
    }

    @Nested
    class searchLightTourists {
        @Test
        void testSearchLightTouristsWithOnePartQuery() {
            String testQuery = "query";
            Tourist tourist = new Tourist();
            tourist.setId(1);
            TouristLightDTO touristDTO = new TouristLightDTO();
            touristDTO.setId(1);
            when(touristRepository.searchByLastNameStartingWithIgnoreCase(testQuery)).thenReturn(List.of(tourist));
            when(touristMapper.toLightDTO(eq(tourist))).thenReturn(touristDTO);

            List<TouristLightDTO> result = touristService.searchLightTourists(testQuery);
            assertEquals(touristDTO, result.getFirst());
        }

        @Test
        void testSearchLightTouristsWithTwoPartQuery() {
            List<String> parts = List.of("part1", "part2");
            String testQuery = Strings.join(parts, ' ');

            Tourist tourist = new Tourist();
            tourist.setId(1);
            TouristLightDTO touristDTO = new TouristLightDTO();
            touristDTO.setId(1);
            when(touristRepository.searchByLastNameStartingWithIgnoreCaseAndFirstNameStartingWithIgnoreCase(parts.getFirst(),
                    parts.getLast())).thenReturn(List.of(tourist));
            when(touristMapper.toLightDTO(eq(tourist))).thenReturn(touristDTO);

            List<TouristLightDTO> result = touristService.searchLightTourists(testQuery);
            assertEquals(touristDTO, result.getFirst());
        }

        @Test
        void testSearchLightTouristsWithFullNameQuery() {
            List<String> parts = List.of("part1", "part2", "part3");
            String testQuery = Strings.join(parts, ' ');

            Tourist tourist = new Tourist();
            tourist.setId(1);
            TouristLightDTO touristDTO = new TouristLightDTO();
            touristDTO.setId(1);
            when(touristRepository.searchByLastNameStartingWithIgnoreCaseAndFirstNameStartingWithIgnoreCaseAndMiddleNameStartingWithIgnoreCase(
                    parts.getFirst(), parts.get(1), parts.getLast())).thenReturn(List.of(tourist));
            when(touristMapper.toLightDTO(eq(tourist))).thenReturn(touristDTO);

            List<TouristLightDTO> result = touristService.searchLightTourists(testQuery);
            assertEquals(touristDTO, result.getFirst());
        }
    }

    @Nested
    class SaveAndReturnTest {
        @Test
        void shouldSaveAndReturnWithoutContactInfo() {
            TouristDTO dto = new TouristDTO();
            Tourist tourist = new Tourist();
            when(touristMapper.toEntity(dto)).thenReturn(tourist);
            when(touristRepository.save(tourist)).thenReturn(tourist);
            TouristLightDTO outDTO = new TouristLightDTO();
            when(touristMapper.toLightDTO(tourist)).thenReturn(outDTO);

            TouristLightDTO lightDTO = touristService.saveAndReturn(dto);
            assertEquals(outDTO, lightDTO);
            verify(touristMapper).toEntity(any(TouristDTO.class));
            verify(touristRepository).save(tourist);
            verify(touristMapper).toLightDTO(tourist);
        }

        @Test
        void shouldSaveAndReturnWithContactInfo() {
            TouristDTO dto = new TouristDTO();
            dto.setContactInfo(new ArrayList<>(List.of(new ContactInfoItemDTO())));
            Tourist tourist = new Tourist();
            tourist.setContactInfo(new ArrayList<>(List.of(new ContactInfoItem())));
            when(touristMapper.toEntity(dto)).thenReturn(tourist);
            when(touristRepository.save(eq(tourist))).thenReturn(tourist);
            TouristLightDTO outDTO = new TouristLightDTO();
            when(touristMapper.toLightDTO(tourist)).thenReturn(outDTO);

            TouristLightDTO lightDTO = touristService.saveAndReturn(dto);

            ArgumentCaptor<Tourist> argTourist = ArgumentCaptor.forClass(Tourist.class);
            assertEquals(outDTO, lightDTO);
            verify(touristMapper).toEntity(any(TouristDTO.class));
            verify(touristRepository).save(argTourist.capture());
            verify(touristMapper).toLightDTO(tourist);
            assertFalse(argTourist.getValue().getContactInfo().isEmpty());
        }
    }

    @Test
    void testFindTouristByIdWhenTouristNotFound() {
        when(touristRepository.findById(anyInt())).thenReturn(Optional.empty());
        assertThrows(TouristNotFoundException.class, () -> touristService.findTouristById(1));
    }

    @Test
    void testFindTouristById() {
        Tourist tourist = new Tourist();
        when(touristRepository.findById(anyInt())).thenReturn(Optional.of(tourist));
        when(protocolRepository.getAssignments(anyList())).thenReturn(List.of());
        TouristDTO dto = new TouristDTO();
        when(touristMapper.toDTO(any(Tourist.class), anyList(), anyList())).thenReturn(dto);

        TouristDTO result = touristService.findTouristById(1);
        assertEquals(dto, result);
    }

    @Test
    void testSave() {
        TouristDTO touristDTO = new TouristDTO();
        touristDTO.setContactInfo(List.of(new ContactInfoItemDTO()));
        Tourist tourist = new Tourist();
        when(touristMapper.toEntity(touristDTO)).thenReturn(tourist);

        touristService.save(touristDTO);
        ArgumentCaptor<Tourist> argTourist = ArgumentCaptor.forClass(Tourist.class);
        verify(touristRepository).save(argTourist.capture());
        assertEquals(tourist, argTourist.getValue());
    }

    @Test
    void testSaveWithoutContactInfo() {
        TouristDTO touristDTO = new TouristDTO();
        Tourist tourist = new Tourist();
        when(touristMapper.toEntity(touristDTO)).thenReturn(tourist);

        touristService.save(touristDTO);
        ArgumentCaptor<Tourist> argTourist = ArgumentCaptor.forClass(Tourist.class);
        verify(touristRepository).save(argTourist.capture());
        assertEquals(tourist, argTourist.getValue());
    }

    @Test
    void testFindTouristByTelegramIdWhenEmpty() {

        when(contactInfoRepository.findTouristIdByTypeAndValue(ContactInfoType.TELEGRAM, String.valueOf(1L))).thenReturn(Optional.empty());
        Optional<TouristDTO> result = touristService.findTouristByTelegramId(1L);
        assertTrue(result.isEmpty());
        verify(touristMapper, never()).toDTO(any(Tourist.class));
    }

    @Test
    void testFindTouristByTelegramId() {
        Tourist tourist = new Tourist();
        ContactInfoItem item = new ContactInfoItem();
        TouristDTO touristDTO = new TouristDTO();
        item.setTourist(tourist);
        when(contactInfoRepository.findTouristIdByTypeAndValue(ContactInfoType.TELEGRAM, String.valueOf(1L))).thenReturn(Optional.of(item));
        when(touristMapper.toDTO(tourist)).thenReturn(touristDTO);

        Optional<TouristDTO> result = touristService.findTouristByTelegramId(1L);

        assertEquals(touristDTO, result.get());
        ArgumentCaptor<Tourist> argTourist = ArgumentCaptor.forClass(Tourist.class);
        verify(touristMapper).toDTO(argTourist.capture());
        assertEquals(tourist, argTourist.getValue());

    }
}