package org.tourism.instructors.application.tourist;

import org.junit.jupiter.api.BeforeEach;
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

    @BeforeEach
    void setUp() {
    }

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
}