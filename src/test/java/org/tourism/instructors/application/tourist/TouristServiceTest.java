package org.tourism.instructors.application.tourist;

import org.apache.logging.log4j.util.Strings;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.tourism.instructors.api.tourist.dto.TouristDTO;
import org.tourism.instructors.api.tourist.dto.TouristLightDTO;
import org.tourism.instructors.api.tourist.mapper.TouristMapper;
import org.tourism.instructors.application.tourist.exception.TouristCannotBeDeletedException;
import org.tourism.instructors.application.tourist.impl.TouristServiceImpl;
import org.tourism.instructors.domain.protocol.repository.ProtocolRepository;
import org.tourism.instructors.domain.tourist.model.Tourist;
import org.tourism.instructors.domain.tourist.repository.ContactInfoRepository;
import org.tourism.instructors.domain.tourist.repository.TouristRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
}