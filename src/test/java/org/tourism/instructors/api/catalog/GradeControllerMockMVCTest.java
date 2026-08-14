package org.tourism.instructors.api.catalog;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.tourism.instructors.api.catalog.dto.GradeDTO;
import org.tourism.instructors.application.catalog.CatalogService;
import org.tourism.instructors.application.catalog.exception.GradeNotFoundException;
import org.tourism.instructors.application.catalog.exception.GradeUsedInProtocolsException;
import org.tourism.instructors.domain.catalog.model.Grade;

@ExtendWith(MockitoExtension.class)
class GradeControllerMockMVCTest {

    @Mock CatalogService catalogService;
    @Mock Grade grade;

    GradeDTO gradeDTO = new GradeDTO(1, "grade", false, 5);

    @InjectMocks GradeController gradeController;

    @InjectMocks GradeExceptionHandler exceptionHandler;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc =
                MockMvcBuilders.standaloneSetup(gradeController)
                        .setControllerAdvice(exceptionHandler)
                        .build();
        lenient().when(grade.getId()).thenReturn(1);
        lenient().when(grade.getTitle()).thenReturn("grade");

        lenient().when(catalogService.findGradeById(1)).thenReturn(gradeDTO);
    }

    @Test
    void deleteGrade() throws Exception {
        doThrow(new GradeUsedInProtocolsException(grade))
                .when(catalogService)
                .deleteGrade(anyInt());
        mockMvc.perform(post("/catalog/grades/1/delete"))
                .andExpect(status().isOk())
                .andExpect(view().name("catalog/grades/view"))
                .andExpect(
                        model().attribute(
                                        "errorMessage",
                                        "Звание grade не может быть удалено "
                                                + "поскольку используется в протоколах"))
                .andExpect(model().attribute("grade", gradeDTO));
    }

    @Test
    void deleteNonExistingGrade() throws Exception {
        doThrow(new GradeNotFoundException(1)).when(catalogService).deleteGrade(1);
        mockMvc.perform(post("/catalog/grades/1/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/catalog/grades"))
                .andExpect(flash().attribute("errorMessage", "Звание с ID:1 не найдено"));
    }
}
