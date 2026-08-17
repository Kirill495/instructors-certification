package org.tourism.instructors.api.protocol;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.tourism.instructors.api.catalog.dto.GradeDTO;
import org.tourism.instructors.api.catalog.dto.KindOfTourismListDTO;
import org.tourism.instructors.api.protocol.dto.ProtocolDTO;
import org.tourism.instructors.api.protocol.dto.ProtocolForListDTO;
import org.tourism.instructors.api.protocol.dto.ProtocolFormDTO;
import org.tourism.instructors.application.catalog.CatalogService;
import org.tourism.instructors.application.protocol.ProtocolService;
import org.tourism.instructors.domain.protocol.ProtocolStatus;

@ExtendWith(MockitoExtension.class)
class ProtocolControllerTest {

    @Mock ProtocolService protocolService;

    @Mock CatalogService catalogService;

    @Mock RedirectAttributes redirectAttributes;

    @InjectMocks ProtocolController controller;

    @Nested
    class ListProtocolsTest {

        Model model = new ExtendedModelMap();

        @Test
        void returnsListView() {
            when(protocolService.getProtocolsForList(eq(null), any()))
                    .thenReturn(new PageImpl<>(List.of()));

            String view = controller.listProtocols(null, null, "asc", 0, 20, null, null, model);

            assertEquals("protocols/list", view);
        }

        @Test
        void returnsTableRowsFragmentForAjaxRequest() {
            when(protocolService.getProtocolsForList(any(), any()))
                    .thenReturn(new PageImpl<>(List.of()));

            String view =
                    controller.listProtocols(
                            null, null, "asc", 0, 20, null, "XMLHttpRequest", model);

            assertEquals("protocols/list :: tableRows", view);
        }

        @Test
        void putsProtocolsAndPaginationAttributesIntoModel() {
            ProtocolForListDTO dto =
                    new ProtocolForListDTO(
                            1, "П-001", LocalDate.now(), "order", ProtocolStatus.DRAFT, List.of());
            when(protocolService.getProtocolsForList(any(), any()))
                    .thenReturn(new PageImpl<>(List.of(dto)));

            controller.listProtocols(null, null, "asc", 0, 20, null, null, model);

            assertEquals(List.of(dto), model.getAttribute("protocols"));
            assertEquals(0, model.getAttribute("currentPage"));
            assertEquals(20, model.getAttribute("pageSize"));
            assertNotNull(model.getAttribute("tourist"));
        }

        @Test
        void passesSearchQueryToServiceAndModel() {
            when(protocolService.getProtocolsForList(eq("abc"), any()))
                    .thenReturn(new PageImpl<>(List.of()));

            controller.listProtocols("abc", null, "asc", 0, 20, null, null, model);

            verify(protocolService).getProtocolsForList(eq("abc"), any());
            assertEquals("abc", model.getAttribute("searchQuery"));
        }

        @Test
        void appliesDefaultAscSortByNumberWhenSortNotProvided() {
            when(protocolService.getProtocolsForList(any(), any()))
                    .thenReturn(new PageImpl<>(List.of()));

            controller.listProtocols(null, null, "asc", 0, 20, null, null, model);

            verify(protocolService)
                    .getProtocolsForList(
                            eq(null),
                            eq(PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "number"))));
        }

        @Test
        void appliesDescSortWhenRequested() {
            when(protocolService.getProtocolsForList(any(), any()))
                    .thenReturn(new PageImpl<>(List.of()));

            controller.listProtocols(null, "date", "desc", 0, 20, null, null, model);

            verify(protocolService)
                    .getProtocolsForList(
                            eq(null),
                            eq(PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "date"))));
        }

        @Test
        void highlightIdAdjustsPageSizeWhenProtocolFound() {
            when(protocolService.getProtocolIndex(5)).thenReturn(42);
            when(protocolService.getProtocolsForList(any(), any()))
                    .thenReturn(new PageImpl<>(List.of()));

            controller.listProtocols(null, null, "asc", 0, 20, 5, null, model);

            int expectedPage = 42 / 20;
            int expectedSize = 20 * (expectedPage + 1);
            verify(protocolService)
                    .getProtocolsForList(
                            eq(null),
                            eq(
                                    PageRequest.of(
                                            0,
                                            expectedSize,
                                            Sort.by(Sort.Direction.ASC, "number"))));
        }

        @Test
        void highlightIdFallsBackToDefaultPageWhenProtocolNotFound() {
            when(protocolService.getProtocolIndex(99)).thenReturn(-1);
            when(protocolService.getProtocolsForList(any(), any()))
                    .thenReturn(new PageImpl<>(List.of()));

            controller.listProtocols(null, null, "asc", 0, 20, 99, null, model);

            verify(protocolService)
                    .getProtocolsForList(
                            eq(null),
                            eq(PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "number"))));
        }

        @Test
        void highlightIdIsIgnoredWhenPageIsNotZero() {
            when(protocolService.getProtocolsForList(any(), any()))
                    .thenReturn(new PageImpl<>(List.of()));

            controller.listProtocols(null, null, "asc", 2, 20, 5, null, model);

            verify(protocolService)
                    .getProtocolsForList(
                            eq(null),
                            eq(PageRequest.of(2, 20, Sort.by(Sort.Direction.ASC, "number"))));
        }
    }

    @Nested
    class PaginationWindowTest {

        Model model = new ExtendedModelMap();

        @Test
        void pageWindowIsCenteredOnCurrentPage() {
            when(protocolService.getProtocolsForList(any(), any()))
                    .thenReturn(new PageImpl<>(List.of(), PageRequest.of(10, 20), 500L));

            controller.listProtocols(null, null, "asc", 10, 20, null, null, model);

            assertEquals(6, model.getAttribute("pageStart"));
            assertEquals(15, model.getAttribute("pageEnd"));
        }

        @Test
        void pageWindowStartsAtZeroOnFirstPage() {
            when(protocolService.getProtocolsForList(any(), any()))
                    .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 500L));

            controller.listProtocols(null, null, "asc", 0, 20, null, null, model);

            assertEquals(0, model.getAttribute("pageStart"));
            assertEquals(9, model.getAttribute("pageEnd"));
        }

        @Test
        void pageWindowEndsAtLastPageOnLastPage() {
            when(protocolService.getProtocolsForList(any(), any()))
                    .thenReturn(new PageImpl<>(List.of(), PageRequest.of(24, 20), 500L));

            controller.listProtocols(null, null, "asc", 24, 20, null, null, model);

            assertEquals(15, model.getAttribute("pageStart"));
            assertEquals(24, model.getAttribute("pageEnd"));
        }
    }

    @Nested
    class ViewProtocolTest {

        Model model = new ExtendedModelMap();

        @Test
        void returnsViewWithProtocol() {
            ProtocolDTO dto =
                    new ProtocolDTO(
                            1, "П-001", LocalDate.now(), "order", ProtocolStatus.DRAFT, List.of());
            when(protocolService.getProtocolById(1)).thenReturn(dto);

            String view = controller.viewProtocol(1, model);

            assertEquals("protocols/view", view);
            assertEquals(dto, model.getAttribute("protocol"));
            assertNotNull(model.getAttribute("tourist"));
        }
    }

    @Nested
    class EditProtocolTest {

        Model model = new ExtendedModelMap();

        @Test
        void returnsEditViewWithFormAndCatalogData() {
            ProtocolFormDTO form = new ProtocolFormDTO();
            form.setId(1);
            GradeDTO grade = new GradeDTO(1, "instructor", true, 10);
            KindOfTourismListDTO kindOfTourism = new KindOfTourismListDTO(1, "cycling");
            when(protocolService.getProtocolFormById(1)).thenReturn(form);
            when(catalogService.findActiveGrades()).thenReturn(List.of(grade));
            when(catalogService.findActiveKindsOfTourism()).thenReturn(List.of(kindOfTourism));

            String view = controller.editProtocol(1, model);

            assertEquals("protocols/edit", view);
            assertEquals(form, model.getAttribute("protocol"));
            assertEquals(List.of(grade), model.getAttribute("grades"));
            assertEquals(List.of(kindOfTourism), model.getAttribute("kindsOfTourism"));
            assertArrayEquals(
                    ProtocolStatus.values(),
                    (ProtocolStatus[]) model.getAttribute("availableStatuses"));
            assertNotNull(model.getAttribute("tourist"));
        }
    }

    @Nested
    class NewProtocolTest {

        Model model = new ExtendedModelMap();

        @Test
        void returnsEditViewWithEmptyFormAndCatalogData() {
            GradeDTO grade = new GradeDTO(1, "instructor", true, 10);
            KindOfTourismListDTO kindOfTourism = new KindOfTourismListDTO(1, "cycling");
            when(catalogService.findActiveGrades()).thenReturn(List.of(grade));
            when(catalogService.findActiveKindsOfTourism()).thenReturn(List.of(kindOfTourism));

            String view = controller.newProtocol(model);

            assertEquals("protocols/edit", view);
            assertNotNull(model.getAttribute("protocol"));
            assertInstanceOf(ProtocolFormDTO.class, model.getAttribute("protocol"));
            assertTrue(((ProtocolFormDTO) model.getAttribute("protocol")).isNewItem());
            assertEquals(List.of(grade), model.getAttribute("grades"));
            assertEquals(List.of(kindOfTourism), model.getAttribute("kindsOfTourism"));
            assertNotNull(model.getAttribute("tourist"));
        }
    }

    @Nested
    class SaveProtocolTest {

        @Test
        void savesProtocolAndRedirects() {
            ProtocolFormDTO form = new ProtocolFormDTO();

            String view = controller.saveProtocol(form, null, redirectAttributes);

            verify(protocolService).saveProtocol(form);
            verify(redirectAttributes).addFlashAttribute("successMessage", "Протокол записан");
            assertEquals("redirect:/protocols", view);
        }
    }

    @Nested
    class DeleteProtocolTest {

        @Test
        void deletesProtocolAndRedirects() {
            String view = controller.deleteProtocol(42, redirectAttributes);

            verify(protocolService).deleteProtocol(42);
            verify(redirectAttributes).addFlashAttribute("successMessage", "Протокол удален");
            assertEquals("redirect:/protocols", view);
        }
    }
}
