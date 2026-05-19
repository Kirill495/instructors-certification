package org.tourism.instructors.api.catalog;

import lombok.AllArgsConstructor;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.tourism.instructors.application.catalog.CatalogService;
import org.tourism.instructors.application.catalog.exception.KindOfTourismNotFoundException;
import org.tourism.instructors.application.catalog.exception.KindOfTourismUsedInProtocolsException;

import static org.tourism.instructors.api.util.CommonAttributes.ERROR_MESSAGE_ATTRIBUTE;

@AllArgsConstructor
@ControllerAdvice(assignableTypes = KindOfTourismController.class)
public class KindOfTourismExceptionHandler {

    private final CatalogService catalogService;

    @ExceptionHandler(KindOfTourismUsedInProtocolsException.class)
    public String handleDeleteNotAllowed(KindOfTourismUsedInProtocolsException e, Model model) {
        model.addAttribute(ERROR_MESSAGE_ATTRIBUTE, e.getMessage());
        model.addAttribute("grade", catalogService.getKindOfTourismById(e.getKindOfTourismId()));
        return "catalog/grades/view";
    }

    @ExceptionHandler(KindOfTourismNotFoundException.class)
    public String handleNotFound(KindOfTourismNotFoundException e, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTRIBUTE, e.getMessage());
        return "redirect:/catalog/kinds-of-tourism";
    }
}
