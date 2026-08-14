package org.tourism.instructors.api.catalog;

import static org.tourism.instructors.api.util.CommonAttributes.ERROR_MESSAGE_ATTRIBUTE;

import lombok.AllArgsConstructor;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.tourism.instructors.application.catalog.CatalogService;
import org.tourism.instructors.application.catalog.exception.GradeNotFoundException;
import org.tourism.instructors.application.catalog.exception.GradeUsedInProtocolsException;

@AllArgsConstructor
@ControllerAdvice(assignableTypes = GradeController.class)
public class GradeExceptionHandler {

    private final CatalogService catalogService;

    @ExceptionHandler(GradeUsedInProtocolsException.class)
    public String handleDeleteNotAllowed(GradeUsedInProtocolsException e, Model model) {
        model.addAttribute(ERROR_MESSAGE_ATTRIBUTE, e.getMessage());
        model.addAttribute("grade", catalogService.findGradeById(e.getGradeId()));
        return "catalog/grades/view";
    }

    @ExceptionHandler(GradeNotFoundException.class)
    public String handleNotFound(GradeNotFoundException e, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTRIBUTE, e.getMessage());
        return "redirect:/catalog/grades";
    }
}
