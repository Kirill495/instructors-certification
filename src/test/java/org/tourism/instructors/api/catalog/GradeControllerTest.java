package org.tourism.instructors.api.catalog;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.tourism.instructors.api.catalog.dto.GradeDTO;
import org.tourism.instructors.application.catalog.CatalogService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GradeControllerTest {

    @Mock
    CatalogService catalogService;

    @Mock
    RedirectAttributes redirectAttributes;

    @InjectMocks
    GradeController controller;

    @Nested
    class ListGradesTest {

        Model model = new ExtendedModelMap();

        @Test
        void returnsListView() {
            when(catalogService.findActiveGrades()).thenReturn(List.of());

            String view = controller.listGrades(false, model);

            assertEquals("catalog/grades/list", view);
        }

        @Test
        void whenShowInactiveFalse_thenLoadsActiveGradesOnly() {
            GradeDTO grade = new GradeDTO(1, "Instructor", false, 5);
            when(catalogService.findActiveGrades()).thenReturn(List.of(grade));

            controller.listGrades(false, model);

            assertEquals(List.of(grade), model.getAttribute("grades"));
            verify(catalogService, never()).findAllGrades();
        }

        @Test
        void whenShowInactiveTrue_thenLoadsAllGrades() {
            GradeDTO grade = new GradeDTO(1, "Instructor", true, 5);
            when(catalogService.findAllGrades()).thenReturn(List.of(grade));

            controller.listGrades(true, model);

            assertEquals(List.of(grade), model.getAttribute("grades"));
            verify(catalogService, never()).findActiveGrades();
        }

        @Test
        void addsShowInactiveFalseToModel() {
            when(catalogService.findActiveGrades()).thenReturn(List.of());

            controller.listGrades(false, model);

            assertEquals(false, model.getAttribute("showInactive"));
        }

        @Test
        void addsShowInactiveTrueToModel() {
            when(catalogService.findAllGrades()).thenReturn(List.of());

            controller.listGrades(true, model);

            assertEquals(true, model.getAttribute("showInactive"));
        }
    }

    @Nested
    class ViewGradeTest {

        Model model = new ExtendedModelMap();

        @Test
        void returnsViewTemplateWithGrade() {
            GradeDTO grade = new GradeDTO(1, "Instructor", false, 5);
            when(catalogService.findGradeById(1)).thenReturn(grade);

            String view = controller.viewGrade(1, model);

            assertEquals("catalog/grades/view", view);
            assertEquals(grade, model.getAttribute("grade"));
        }
    }

    @Nested
    class EditGradeFormTest {

        Model model = new ExtendedModelMap();

        @Test
        void returnsEditTemplateWithGrade() {
            GradeDTO grade = new GradeDTO(1, "Instructor", false, 5);
            when(catalogService.findGradeById(1)).thenReturn(grade);

            String view = controller.editGrade(1, model);

            assertEquals("catalog/grades/edit", view);
            assertEquals(grade, model.getAttribute("grade"));
        }
    }

    @Nested
    class UpdateGradeTest {

        @Test
        void savesGradeUsingPathVariableIdAndRedirects() {
            GradeDTO form = new GradeDTO(99, "Master", true, 3);

            String view = controller.updateGrade(1, form, redirectAttributes);

            ArgumentCaptor<GradeDTO> captor = ArgumentCaptor.forClass(GradeDTO.class);
            verify(catalogService).saveGrade(captor.capture());
            GradeDTO saved = captor.getValue();
            assertEquals(1, saved.id());
            assertEquals("Master", saved.title());
            assertTrue(saved.inactive());
            assertEquals(3, saved.expiresInYears());
            assertEquals("redirect:/catalog/grades", view);
        }

        @Test
        void addsSuccessFlashAttribute() {
            controller.updateGrade(1, new GradeDTO(1, "T", false, 0), redirectAttributes);

            verify(redirectAttributes).addFlashAttribute("successMessage", "Звание успешно обновлено");
        }
    }

    @Nested
    class NewGradeFormTest {

        Model model = new ExtendedModelMap();

        @Test
        void returnsEditTemplateWithEmptyGrade() {
            String view = controller.newGradeForm(model);

            assertEquals("catalog/grades/edit", view);
            GradeDTO grade = (GradeDTO) model.getAttribute("grade");
            assertNotNull(grade);
            assertTrue(grade.isNewItem());
            assertNull(grade.title());
            assertFalse(grade.inactive());
            assertEquals(0, grade.expiresInYears());
        }
    }

    @Nested
    class CreateGradeTest {

        @Test
        void savesGradeAndRedirects() {
            GradeDTO dto = new GradeDTO(null, "Instructor", false, 5);

            String view = controller.createGrade(dto, redirectAttributes);

            verify(catalogService).saveGrade(dto);
            assertEquals("redirect:/catalog/grades", view);
        }

        @Test
        void addsSuccessFlashAttribute() {
            controller.createGrade(new GradeDTO(null, "T", false, 0), redirectAttributes);

            verify(redirectAttributes).addFlashAttribute("successMessage", "Звание успешно создано");
        }
    }

    @Nested
    class DeleteGradeTest {

        @Test
        void deletesGradeAndRedirects() {
            String view = controller.deleteGrade(7, redirectAttributes);

            verify(catalogService).deleteGrade(7);
            assertEquals("redirect:/catalog/grades", view);
        }

        @Test
        void addsSuccessFlashAttribute() {
            controller.deleteGrade(7, redirectAttributes);

            verify(redirectAttributes).addFlashAttribute("successMessage", "Звание успешно удалено");
        }
    }
}