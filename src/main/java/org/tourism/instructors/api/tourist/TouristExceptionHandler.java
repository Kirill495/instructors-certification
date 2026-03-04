package org.tourism.instructors.api.tourist;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.tourism.instructors.application.tourist.TouristService;
import org.tourism.instructors.application.tourist.exception.TouristCannotBeDeletedException;

@ControllerAdvice(assignableTypes = TouristController.class)
public class TouristExceptionHandler {

    private final TouristService touristService;

    public TouristExceptionHandler (TouristService touristService) {
        this.touristService = touristService;
    }

    @ExceptionHandler(TouristCannotBeDeletedException.class)
    public String handleDeletionNotAllowed (TouristCannotBeDeletedException exception, Model model) {
        model.addAttribute("errorMessage", exception.getMessage());
        model.addAttribute("tourist", touristService.findTouristById(exception.getTouristId()));
        return "tourists/view";
    }

}
