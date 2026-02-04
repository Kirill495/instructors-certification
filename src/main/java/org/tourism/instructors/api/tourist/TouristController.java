package org.tourism.instructors.api.tourist;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.tourism.instructors.domain.tourist.TouristService;

@Controller
@RequestMapping("/tourists")
public class TouristController {

    private final TouristService touristService;

    public TouristController (TouristService touristService) {
        this.touristService = touristService;
    }

    @GetMapping
    public String listTourists(Model model) {
        model.addAttribute("tourists", touristService.getAllTourists());
        return "tourists/list";
    }
}
