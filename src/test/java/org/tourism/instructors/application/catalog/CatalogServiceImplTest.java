package org.tourism.instructors.application.catalog;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.tourism.instructors.api.catalog.dto.GradeDTO;
import org.tourism.instructors.api.catalog.dto.KindOfTourismDTO;
import org.tourism.instructors.api.catalog.dto.KindOfTourismListDTO;
import org.tourism.instructors.api.catalog.mapper.GradeMapper;
import org.tourism.instructors.api.catalog.mapper.KindOfTourismMapper;
import org.tourism.instructors.application.catalog.exception.GradeNotFoundException;
import org.tourism.instructors.application.catalog.exception.KindOfTourismNotFoundException;
import org.tourism.instructors.application.catalog.impl.CatalogServiceImpl;
import org.tourism.instructors.domain.catalog.model.Grade;
import org.tourism.instructors.domain.catalog.model.KindOfTourism;
import org.tourism.instructors.domain.catalog.repository.GradeRepository;
import org.tourism.instructors.domain.catalog.repository.KindOfTourismRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CatalogServiceImplTest {

    @Mock
    KindOfTourismRepository kindOfTourismRepository;
    @Mock
    GradeRepository gradeRepository;
    @Mock
    KindOfTourismMapper kindOfTourismMapper;
    @Mock
    GradeMapper gradeMapper;

    @InjectMocks
    CatalogServiceImpl catalogService;

    // --- KindOfTourism ---

    @Nested
    class CountActiveKindsOfTourism {

        @Test
        void returnsCountFromRepository() {
            when(kindOfTourismRepository.countKindOfTourismByInactive(false)).thenReturn(5L);
            assertEquals(5, catalogService.countActiveKindsOfTourism());
        }

        @Test
        void returnsZeroWhenNoneActive() {
            when(kindOfTourismRepository.countKindOfTourismByInactive(false)).thenReturn(0L);
            assertEquals(0, catalogService.countActiveKindsOfTourism());
        }
    }

    @Nested
    class FindAllKindsOfTourism {

        @Test
        void returnsMappedList() {
            KindOfTourism entity = new KindOfTourism(1, "Hiking", false);
            KindOfTourismListDTO dto = new KindOfTourismListDTO(1, "Hiking");
            when(kindOfTourismRepository.findAllByOrderByIdAsc()).thenReturn(List.of(entity));
            when(kindOfTourismMapper.toListDTO(entity)).thenReturn(dto);

            List<KindOfTourismListDTO> result = catalogService.findAllKindsOfTourism();

            assertEquals(1, result.size());
            assertEquals(dto, result.getFirst());
        }

        @Test
        void returnsEmptyListWhenRepositoryEmpty() {
            when(kindOfTourismRepository.findAllByOrderByIdAsc()).thenReturn(List.of());

            List<KindOfTourismListDTO> result = catalogService.findAllKindsOfTourism();

            assertTrue(result.isEmpty());
            verify(kindOfTourismMapper, never()).toListDTO(any());
        }
    }

    @Nested
    class FindActiveKindsOfTourism {

        @Test
        void returnsMappedActiveList() {
            KindOfTourism entity = new KindOfTourism(2, "Climbing", false);
            KindOfTourismListDTO dto = new KindOfTourismListDTO(2, "Climbing");
            when(kindOfTourismRepository.findByInactiveFalseOrderByIdAsc()).thenReturn(List.of(entity));
            when(kindOfTourismMapper.toListDTO(entity)).thenReturn(dto);

            List<KindOfTourismListDTO> result = catalogService.findActiveKindsOfTourism();

            assertEquals(1, result.size());
            assertEquals(dto, result.getFirst());
        }

        @Test
        void returnsEmptyListWhenNoActiveItems() {
            when(kindOfTourismRepository.findByInactiveFalseOrderByIdAsc()).thenReturn(List.of());

            assertTrue(catalogService.findActiveKindsOfTourism().isEmpty());
        }
    }

    @Nested
    class GetKindOfTourismById {

        @Test
        void returnsDtoWhenFound() {
            KindOfTourism entity = new KindOfTourism(1, "Skiing", false);
            KindOfTourismDTO dto = new KindOfTourismDTO(1, "Skiing", false);
            when(kindOfTourismRepository.findById(1)).thenReturn(Optional.of(entity));
            when(kindOfTourismMapper.toDTO(entity)).thenReturn(dto);

            KindOfTourismDTO result = catalogService.getKindOfTourismById(1);

            assertEquals(dto, result);
        }

        @Test
        void throwsExceptionWhenNotFound() {
            when(kindOfTourismRepository.findById(99)).thenReturn(Optional.empty());
            assertThrows(KindOfTourismNotFoundException.class, () -> catalogService.getKindOfTourismById(99));
        }
    }

    @Nested
    class SaveKindOfTourism {

        @Test
        void mapsAndSavesEntity() {
            KindOfTourismDTO dto = new KindOfTourismDTO(null, "Rafting", false);
            KindOfTourism entity = new KindOfTourism(null, "Rafting", false);
            when(kindOfTourismMapper.toEntity(dto)).thenReturn(entity);

            catalogService.saveKindOfTourism(dto);

            verify(kindOfTourismRepository).save(entity);
        }
    }

    @Nested
    class DeleteKindOfTourism {

        @Test
        void delegatesToRepository() {
            catalogService.deleteKindOfTourism(3);
            verify(kindOfTourismRepository).deleteById(3);
        }
    }

    // --- Grade ---

    @Nested
    class CountActiveGrades {

        @Test
        void returnsCountFromRepository() {
            when(gradeRepository.countGradesByInactive(false)).thenReturn(7);
            assertEquals(7, catalogService.countActiveGrades());
        }

        @Test
        void returnsZeroWhenNoneActive() {
            when(gradeRepository.countGradesByInactive(false)).thenReturn(0);
            assertEquals(0, catalogService.countActiveGrades());
        }
    }

    @Nested
    class FindAllGrades {

        @Test
        void returnsMappedList() {
            Grade entity = new Grade();
            entity.setId(1);
            entity.setTitle("3rd grade");
            GradeDTO dto = new GradeDTO(1, "3rd grade", false, 3);
            when(gradeRepository.findAllByOrderById()).thenReturn(List.of(entity));
            when(gradeMapper.toDTO(entity)).thenReturn(dto);

            List<GradeDTO> result = catalogService.findAllGrades();

            assertEquals(1, result.size());
            assertEquals(dto, result.getFirst());
        }

        @Test
        void returnsEmptyListWhenRepositoryEmpty() {
            when(gradeRepository.findAllByOrderById()).thenReturn(List.of());

            assertTrue(catalogService.findAllGrades().isEmpty());
            verify(gradeMapper, never()).toDTO(any());
        }
    }

    @Nested
    class FindActiveGrades {

        @Test
        void returnsMappedActiveList() {
            Grade entity = new Grade();
            entity.setId(2);
            GradeDTO dto = new GradeDTO(2, "2nd grade", false, 5);
            when(gradeRepository.findByInactiveFalseOrderById()).thenReturn(List.of(entity));
            when(gradeMapper.toDTO(entity)).thenReturn(dto);

            List<GradeDTO> result = catalogService.findActiveGrades();

            assertEquals(1, result.size());
            assertEquals(dto, result.getFirst());
        }

        @Test
        void returnsEmptyListWhenNoActiveGrades() {
            when(gradeRepository.findByInactiveFalseOrderById()).thenReturn(List.of());

            assertTrue(catalogService.findActiveGrades().isEmpty());
        }
    }

    @Nested
    class FindGradeById {

        @Test
        void returnsDtoWhenFound() {
            Grade entity = new Grade();
            entity.setId(1);
            GradeDTO dto = new GradeDTO(1, "1st grade", false, 10);
            when(gradeRepository.findById(1)).thenReturn(Optional.of(entity));
            when(gradeMapper.toDTO(entity)).thenReturn(dto);

            GradeDTO result = catalogService.findGradeById(1);

            assertEquals(dto, result);
        }

        @Test
        void throwsExceptionWhenNotFound() {
            when(gradeRepository.findById(99)).thenReturn(Optional.empty());
            assertThrows(GradeNotFoundException.class, () -> catalogService.findGradeById(99));
        }
    }

    @Nested
    class SaveGrade {

        @Test
        void mapsAndSavesEntity() {
            GradeDTO dto = new GradeDTO(null, "Master", false, 0);
            Grade entity = new Grade();
            when(gradeMapper.toEntity(dto)).thenReturn(entity);

            catalogService.saveGrade(dto);

            verify(gradeRepository).save(entity);
        }
    }

    @Nested
    class DeleteGrade {

        @Test
        void delegatesToRepository() {
            catalogService.deleteGrade(5);
            verify(gradeRepository).deleteById(5);
        }
    }
}