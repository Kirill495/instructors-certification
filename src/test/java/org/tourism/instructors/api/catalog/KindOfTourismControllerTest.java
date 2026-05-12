package org.tourism.instructors.api.catalog;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.tourism.instructors.api.catalog.dto.KindOfTourismDTO;
import org.tourism.instructors.api.catalog.dto.KindOfTourismListDTO;
import org.tourism.instructors.application.catalog.CatalogService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class KindOfTourismControllerTest {

    @Mock
    CatalogService catalogService;

    @InjectMocks
    KindOfTourismController controller;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Nested
    class ListKindsOfTourism {

        @Test
        void withoutShowInactive_callsFindActiveKindsOfTourism() throws Exception {
            mockMvc.perform(get("/catalog/kinds-of-tourism"));

            verify(catalogService).findActiveKindsOfTourism();
            verify(catalogService, never()).findAllKindsOfTourism();
        }

        @Test
        void withoutShowInactive_returnsListView() throws Exception {
            mockMvc.perform(get("/catalog/kinds-of-tourism"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("catalog/kinds-of-tourism/list"));
        }

        @Test
        void withoutShowInactive_addsKindsAndFlagToModel() throws Exception {
            List<KindOfTourismListDTO> kinds = List.of(new KindOfTourismListDTO(1, "Hiking"));
            when(catalogService.findActiveKindsOfTourism()).thenReturn(kinds);

            mockMvc.perform(get("/catalog/kinds-of-tourism"))
                    .andExpect(model().attribute("kindsOfTourism", kinds))
                    .andExpect(model().attribute("showInactive", false));
        }

        @Test
        void withShowInactiveTrue_callsFindAllKindsOfTourism() throws Exception {
            mockMvc.perform(get("/catalog/kinds-of-tourism").param("showInactive", "true"));

            verify(catalogService).findAllKindsOfTourism();
            verify(catalogService, never()).findActiveKindsOfTourism();
        }

        @Test
        void withShowInactiveTrue_addsAllKindsAndFlagToModel() throws Exception {
            List<KindOfTourismListDTO> kinds = List.of(
                    new KindOfTourismListDTO(1, "Hiking"),
                    new KindOfTourismListDTO(2, "Cycling"));
            when(catalogService.findAllKindsOfTourism()).thenReturn(kinds);

            mockMvc.perform(get("/catalog/kinds-of-tourism").param("showInactive", "true"))
                    .andExpect(model().attribute("kindsOfTourism", kinds))
                    .andExpect(model().attribute("showInactive", true));
        }
    }

    @Nested
    class ViewKindOfTourism {

        @Test
        void returnsViewWithKindInModel() throws Exception {
            KindOfTourismDTO dto = new KindOfTourismDTO(3, "Cycling", false);
            when(catalogService.getKindOfTourismById(3)).thenReturn(dto);

            mockMvc.perform(get("/catalog/kinds-of-tourism/3"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("catalog/kinds-of-tourism/view"))
                    .andExpect(model().attribute("kindOfTourism", dto));
        }
    }

    @Nested
    class EditKindOfTourismForm {

        @Test
        void returnsEditViewWithKindInModel() throws Exception {
            KindOfTourismDTO dto = new KindOfTourismDTO(5, "Hiking", false);
            when(catalogService.getKindOfTourismById(5)).thenReturn(dto);

            mockMvc.perform(get("/catalog/kinds-of-tourism/5/edit"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("catalog/kinds-of-tourism/edit"))
                    .andExpect(model().attribute("kindOfTourism", dto));
        }
    }

    @Nested
    class UpdateKindOfTourism {

        @Test
        void setsIdOnDtoBeforeSaving() throws Exception {
            mockMvc.perform(post("/catalog/kinds-of-tourism/7/edit"));

            ArgumentCaptor<KindOfTourismDTO> captor = ArgumentCaptor.forClass(KindOfTourismDTO.class);
            verify(catalogService).saveKindOfTourism(captor.capture());
            assertEquals(7, captor.getValue().getId());
        }

        @Test
        void redirectsWithSuccessMessage() throws Exception {
            mockMvc.perform(post("/catalog/kinds-of-tourism/7/edit"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/catalog/kinds-of-tourism"))
                    .andExpect(flash().attribute("successMessage", "Вид туризма успешно обновлен"));
        }
    }

    @Nested
    class NewKindOfTourismForm {

        @Test
        void returnsEditViewWithEmptyDto() throws Exception {
            mockMvc.perform(get("/catalog/kinds-of-tourism/new"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("catalog/kinds-of-tourism/edit"))
                    .andExpect(model().attributeExists("kindOfTourism"));
        }
    }

    @Nested
    class CreateKindOfTourism {

        @Test
        void callsSaveAndRedirectsWithSuccessMessage() throws Exception {
            mockMvc.perform(post("/catalog/kinds-of-tourism/new"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/catalog/kinds-of-tourism"))
                    .andExpect(flash().attribute("successMessage", "Вид туризма успешно создан"));

            verify(catalogService).saveKindOfTourism(any(KindOfTourismDTO.class));
        }
    }

    @Nested
    class DeleteKindOfTourism {

        @Test
        void callsDeleteAndRedirectsWithSuccessMessage() throws Exception {
            mockMvc.perform(post("/catalog/kinds-of-tourism/11/delete"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/catalog/kinds-of-tourism"))
                    .andExpect(flash().attribute("successMessage", "Вид туризма успешно удален"));

            verify(catalogService).deleteKindOfTourism(11);
        }
    }
}