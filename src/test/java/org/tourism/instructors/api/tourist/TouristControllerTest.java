package org.tourism.instructors.api.tourist;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.tourism.instructors.api.tourist.dto.TouristDTO;
import org.tourism.instructors.application.tourist.TouristService;

@ExtendWith(MockitoExtension.class)
class TouristControllerTest {

    @Mock TouristService touristService;

    @InjectMocks TouristController controller;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Nested
    class ListTourists {

        @BeforeEach
        void stubService() {
            lenient()
                    .when(touristService.getAllTourists(any()))
                    .thenReturn(new PageImpl<>(List.of()));
        }

        @Test
        void noParams_callsGetAllTouristsWithDefaultSort() throws Exception {
            mockMvc.perform(get("/tourists"));

            verify(touristService)
                    .getAllTourists(
                            PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "certificationId")));
        }

        @Test
        void noParams_returnsListView() throws Exception {
            mockMvc.perform(get("/tourists"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("tourists/list"));
        }

        @Test
        void noParams_addsTouristAttributeToModel() throws Exception {
            mockMvc.perform(get("/tourists")).andExpect(model().attributeExists("tourist"));
        }

        @Test
        void ajaxRequest_returnsTableRowsFragment() throws Exception {
            mockMvc.perform(get("/tourists").header("X-Requested-With", "XMLHttpRequest"))
                    .andExpect(view().name("tourists/list :: tableRows"));
        }

        @Test
        void ajaxRequest_doesNotAddTouristAttribute() throws Exception {
            mockMvc.perform(get("/tourists").header("X-Requested-With", "XMLHttpRequest"))
                    .andExpect(model().attributeDoesNotExist("tourist"));
        }

        @Test
        void withNonBlankSearch_callsSearchTourists() throws Exception {
            when(touristService.searchTourists(eq("Иванов"), any()))
                    .thenReturn(new PageImpl<>(List.of()));

            mockMvc.perform(get("/tourists").param("search", "Иванов"));

            verify(touristService).searchTourists(eq("Иванов"), any());
            verify(touristService, never()).getAllTourists(any());
        }

        @Test
        void withBlankSearch_callsGetAllTourists() throws Exception {
            mockMvc.perform(get("/tourists").param("search", "   "));

            verify(touristService).getAllTourists(any());
            verify(touristService, never()).searchTourists(any(), any());
        }

        @Test
        void descOrder_appliesDescSort() throws Exception {
            mockMvc.perform(get("/tourists").param("sort", "lastName").param("order", "desc"));

            verify(touristService)
                    .getAllTourists(
                            PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "lastName")));
        }

        @Test
        void customPageAndSize_passedToPageable() throws Exception {
            mockMvc.perform(get("/tourists").param("page", "2").param("size", "5"));

            verify(touristService)
                    .getAllTourists(
                            PageRequest.of(2, 5, Sort.by(Sort.Direction.ASC, "certificationId")));
        }

        @Test
        void paginationAttributesAddedToModel() throws Exception {
            when(touristService.getAllTourists(any()))
                    .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 42L));

            mockMvc.perform(get("/tourists").param("page", "0").param("size", "20"))
                    .andExpect(model().attribute("currentPage", 0))
                    .andExpect(model().attribute("totalElements", 42L))
                    .andExpect(model().attribute("pageSize", 20))
                    .andExpect(model().attribute("sortField", "certificationId"))
                    .andExpect(model().attribute("sortOrder", "asc"));
        }

        @Test
        void searchQueryPreservedInModel() throws Exception {
            when(touristService.searchTourists(eq("abc"), any()))
                    .thenReturn(new PageImpl<>(List.of()));

            mockMvc.perform(get("/tourists").param("search", "abc"))
                    .andExpect(model().attribute("searchQuery", "abc"));
        }
    }

    @Nested
    class ViewTourist {

        @Test
        void returnsViewWithTouristInModel() throws Exception {
            TouristDTO dto = new TouristDTO();
            dto.setId(7);
            when(touristService.findTouristById(7)).thenReturn(dto);

            mockMvc.perform(get("/tourists/7"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("tourists/view"))
                    .andExpect(model().attribute("tourist", dto));
        }
    }

    @Nested
    class TouristFragment {

        @Test
        void returnsTouristCardFragment() throws Exception {
            TouristDTO dto = new TouristDTO();
            when(touristService.findTouristById(3)).thenReturn(dto);

            mockMvc.perform(get("/tourists/3/fragment"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("tourists/view :: touristCard"))
                    .andExpect(model().attribute("tourist", dto));
        }
    }

    @Nested
    class TouristEditFragment {

        @Test
        void returnsTouristFormFieldsFragment() throws Exception {
            TouristDTO dto = new TouristDTO();
            when(touristService.findTouristById(5)).thenReturn(dto);

            mockMvc.perform(get("/tourists/5/edit-fragment"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("tourists/edit :: touristFormFields"))
                    .andExpect(model().attribute("tourist", dto));
        }
    }

    @Nested
    class EditTourist {

        @Test
        void returnsEditViewWithTouristInModel() throws Exception {
            TouristDTO dto = new TouristDTO();
            when(touristService.findTouristById(4)).thenReturn(dto);

            mockMvc.perform(get("/tourists/4/edit"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("tourists/edit"))
                    .andExpect(model().attribute("tourist", dto));
        }
    }

    @Nested
    class NewTourist {

        @Test
        void getReturnsEditViewWithEmptyTourist() throws Exception {
            mockMvc.perform(get("/tourists/new"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("tourists/edit"))
                    .andExpect(model().attributeExists("tourist"));
        }

        @Test
        void postSavesTouristAndRedirects() throws Exception {
            mockMvc.perform(post("/tourists/new"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/tourists"))
                    .andExpect(flash().attribute("successMessage", "Данные туриста сохранены"));

            verify(touristService).save(any(TouristDTO.class));
        }
    }

    @Nested
    class UpdateTourist {

        @Test
        void setsIdOnTouristBeforeSaving() throws Exception {
            mockMvc.perform(post("/tourists/12/edit"));

            ArgumentCaptor<TouristDTO> captor = ArgumentCaptor.forClass(TouristDTO.class);
            verify(touristService).save(captor.capture());
            assertEquals(12, captor.getValue().getId());
        }

        @Test
        void redirectsWithSuccessMessage() throws Exception {
            mockMvc.perform(post("/tourists/12/edit"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/tourists"))
                    .andExpect(
                            flash().attribute(
                                            "successMessage", "Данные туриста успешно обновлены"));
        }
    }

    @Nested
    class DeleteTourist {

        @Test
        void callsServiceAndRedirects() throws Exception {
            mockMvc.perform(post("/tourists/9/delete"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/tourists"))
                    .andExpect(flash().attribute("successMessage", "Сведения о туристе удалены"));

            verify(touristService).delete(9);
        }
    }
}
