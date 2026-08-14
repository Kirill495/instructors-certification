package org.tourism.instructors.api.bot;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.tourism.instructors.application.catalog.CatalogService;

@Controller
@RequestMapping("/bot")
public class MiniAppController {

    private final CatalogService catalogService;

    public MiniAppController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping({"", "/register"})
    public String registerForm(Model model) {
        model.addAttribute("kindsOfTourism", catalogService.findActiveKindsOfTourism());
        model.addAttribute("grades", catalogService.findActiveGrades());
        return "bot/register";
    }
}
