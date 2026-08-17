package org.tourism.instructors.api.tourist;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.util.List;
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
import org.springframework.data.domain.Sort;
import org.tourism.instructors.api.tourist.dto.TouristDTO;
import org.tourism.instructors.api.tourist.dto.TouristSummaryDTO;
import org.tourism.instructors.application.tourist.TouristService;

@ExtendWith(MockitoExtension.class)
class TouristRestControllerTest {

    @Mock TouristService touristService;

    @InjectMocks TouristRestController controller;

    TouristSummaryDTO summary =
            new TouristSummaryDTO(1, "Ivan", "Petrov", "Alekseevich", "CERT-001");

    @Nested
    class CreateTouristTest {

        @Test
        void delegatesToServiceAndReturnsCreatedSummary() {
            TouristDTO dto = new TouristDTO();
            when(touristService.saveAndReturn(dto)).thenReturn(summary);

            TouristSummaryDTO result = controller.createTourist(dto);

            verify(touristService).saveAndReturn(dto);
            assertEquals(summary, result);
        }
    }

    @Nested
    class UpdateTouristTest {

        @Test
        void setsIdOnDtoThenDelegatesToService() {
            TouristDTO dto = new TouristDTO();
            when(touristService.saveAndReturn(dto)).thenReturn(summary);

            TouristSummaryDTO result = controller.updateTourist(42, dto);

            assertEquals(42, dto.getId());
            verify(touristService).saveAndReturn(dto);
            assertEquals(summary, result);
        }
    }

    @Nested
    class SearchTouristsTest {

        @Test
        void returnsEmptyListWhenQueryIsBlank() {
            List<TouristSummaryDTO> result = controller.searchTourists("   ");

            assertTrue(result.isEmpty());
            verifyNoInteractions(touristService);
        }

        @Test
        void returnsEmptyListWhenQueryIsEmpty() {
            List<TouristSummaryDTO> result = controller.searchTourists("");

            assertTrue(result.isEmpty());
            verifyNoInteractions(touristService);
        }

        @Test
        void delegatesToServiceAndReturnsResultsForNonBlankQuery() {
            when(touristService.searchLightTourists("petrov")).thenReturn(List.of(summary));

            List<TouristSummaryDTO> result = controller.searchTourists("petrov");

            assertEquals(List.of(summary), result);
            verify(touristService).searchLightTourists("petrov");
        }
    }

    @Nested
    class ListTouristsTest {

        @Test
        void callsGetAllTouristsWhenSearchIsNull() {
            Page<TouristDTO> page = new PageImpl<>(List.of());
            Pageable expected =
                    PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "certificationId"));
            when(touristService.getAllTourists(expected)).thenReturn(page);

            Page<TouristDTO> result =
                    controller.listTourists(null, "certificationId", "asc", 0, 20);

            verify(touristService).getAllTourists(expected);
            verify(touristService, never()).searchTourists(any(), any());
            assertEquals(page, result);
        }

        @Test
        void callsGetAllTouristsWhenSearchIsBlank() {
            Page<TouristDTO> page = new PageImpl<>(List.of());
            when(touristService.getAllTourists(any())).thenReturn(page);

            controller.listTourists("   ", "certificationId", "asc", 0, 20);

            verify(touristService).getAllTourists(any());
            verify(touristService, never()).searchTourists(any(), any());
        }

        @Test
        void callsSearchTouristsWhenSearchIsProvided() {
            Page<TouristDTO> page = new PageImpl<>(List.of());
            Pageable expected =
                    PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "certificationId"));
            when(touristService.searchTourists(eq("petrov"), eq(expected))).thenReturn(page);

            Page<TouristDTO> result =
                    controller.listTourists("petrov", "certificationId", "asc", 0, 20);

            verify(touristService).searchTourists("petrov", expected);
            verify(touristService, never()).getAllTourists(any());
            assertEquals(page, result);
        }

        @Test
        void appliesDescSortWhenOrderIsDesc() {
            Page<TouristDTO> page = new PageImpl<>(List.of());
            Pageable expected = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "lastName"));
            when(touristService.getAllTourists(expected)).thenReturn(page);

            controller.listTourists(null, "lastName", "desc", 0, 20);

            verify(touristService).getAllTourists(expected);
        }

        @Test
        void respectsPageAndSizeParameters() {
            Page<TouristDTO> page = new PageImpl<>(List.of());
            Pageable expected =
                    PageRequest.of(3, 10, Sort.by(Sort.Direction.ASC, "certificationId"));
            when(touristService.getAllTourists(expected)).thenReturn(page);

            controller.listTourists(null, "certificationId", "asc", 3, 10);

            verify(touristService).getAllTourists(expected);
        }
    }
}
