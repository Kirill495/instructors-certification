package org.tourism.instructors.api.tourist;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.tourism.instructors.application.tourist.TouristService;
import org.tourism.instructors.application.tourist.exception.TouristCannotBeDeletedException;
import org.tourism.instructors.application.tourist.exception.TouristNotFoundException;

import static org.tourism.instructors.api.util.CommonAttributes.ERROR_MESSAGE_ATTRIBUTE;

@ControllerAdvice(assignableTypes = TouristController.class)
public class TouristExceptionHandler {

    private final TouristService touristService;

    public TouristExceptionHandler (TouristService touristService) {
        this.touristService = touristService;
    }

    @ExceptionHandler(TouristCannotBeDeletedException.class)
    public String handleDeletionNotAllowed (TouristCannotBeDeletedException exception, Model model) {
        model.addAttribute(ERROR_MESSAGE_ATTRIBUTE, exception.getMessage());
        model.addAttribute("tourist", touristService.findTouristById(exception.getTouristId()));
        return "tourists/view";
    }

    @ExceptionHandler(TouristNotFoundException.class)
    public String handleNotFoundException (TouristNotFoundException exception, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTRIBUTE, exception.getMessage());
        return "redirect:/tourists";
    }

}
