package org.tourism.instructors.api.catalog;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.tourism.instructors.api.catalog.dto.KindOfTourismDTO;
import org.tourism.instructors.application.catalog.CatalogService;

@Controller
@RequestMapping("/catalog/kinds-of-tourism")
public class KindOfTourismController {

    private final CatalogService catalogService;

    public KindOfTourismController (CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping
    public String listKindsOfTourism(@RequestParam(required = false, defaultValue = "false") boolean showInactive,
                                     Model model) {
        if (showInactive) {
            model.addAttribute("kindsOfTourism", catalogService.findAllKindsOfTourism());
        } else {
            model.addAttribute("kindsOfTourism", catalogService.findActiveKindsOfTourism());
        }
        model.addAttribute("showInactive", showInactive);
        return "catalog/kinds-of-tourism/list";
    }

    @GetMapping("/{id}")
    public String viewKindOfTourism(@PathVariable int id, Model model) {
        model.addAttribute("kindOfTourism", catalogService.getKindOfTourismById(id));
        return "catalog/kinds-of-tourism/view";
    }

    @GetMapping("/{id}/edit")
    public String editKindOfTourism(@PathVariable int id, Model model) {
        model.addAttribute("kindOfTourism", catalogService.getKindOfTourismById(id));
        return "catalog/kinds-of-tourism/edit";
    }

    @PostMapping("/{id}/edit")
    public String updateKindOfTourism(@PathVariable int id,
                                      @ModelAttribute KindOfTourismDTO kindOfTourism,
                                      RedirectAttributes redirectAttributes) {
        kindOfTourism.setId(id);
        catalogService.saveKindOfTourism(kindOfTourism);
        redirectAttributes.addFlashAttribute("successMessage", "Вид туризма успешно обновлен");
        return "redirect:/catalog/kinds-of-tourism";
    }

    @GetMapping("/new")
    public String newKindOfTourismForm(Model model) {
        model.addAttribute("kindOfTourism", new KindOfTourismDTO());
        return "catalog/kinds-of-tourism/edit";
    }

    @PostMapping("/new")
    public String createKindOfTourism(@ModelAttribute KindOfTourismDTO kindOfTourism,
                                      RedirectAttributes redirectAttributes) {
        catalogService.saveKindOfTourism(kindOfTourism);
        redirectAttributes.addFlashAttribute("successMessage", "Вид туризма успешно создан");
        return "redirect:/catalog/kinds-of-tourism";
    }

    @PostMapping("/{id}/delete")
    public String deleteKindOfTourism(@PathVariable int id,
                                      RedirectAttributes redirectAttributes) {
        catalogService.deleteKindOfTourism(id);
        redirectAttributes.addFlashAttribute("successMessage", "Вид туризма успешно удален");
        return "redirect:/catalog/kinds-of-tourism";
    }
}
