package org.tourism.instructors.api.tourist;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.tourism.instructors.api.tourist.dto.TouristDTO;
import org.tourism.instructors.domain.tourist.TouristService;

import java.util.List;

@Controller
@RequestMapping("/tourists")
public class TouristController {

    private final TouristService touristService;

    public TouristController (TouristService touristService) {
        this.touristService = touristService;
    }

    @GetMapping
    public String listTourists(@RequestParam(value = "search", required = false) String searchParam, Model model) {
        List<TouristDTO> tourists;
        if (searchParam != null && !searchParam.trim().isEmpty()) {
            tourists = touristService.searchTourists(searchParam);
        } else {
            tourists = touristService.getAllTourists();
        }
        model.addAttribute("tourists", tourists);
        model.addAttribute("search", searchParam);
        return "tourists/list";
    }

    @GetMapping("/{id}")
    public String viewTourist(@PathVariable int id,  Model model) {
        model.addAttribute("tourist", touristService.findTouristById(id));
        return "tourists/view";
    }

    @GetMapping("/{id}/edit")
    public String editTourist(@PathVariable int id, Model model) {
        model.addAttribute("tourist", touristService.findTouristById(id));
        return "tourists/edit";
    }

    @GetMapping("/new")
    public String newTourist(Model model) {
        model.addAttribute("tourist", new TouristDTO());
        return "tourists/edit";
    }

    @PostMapping("/{id}/edit")
    public String updateTourist(@PathVariable int id,
                                @ModelAttribute TouristDTO tourist,
                                RedirectAttributes redirectAttributes) {
        tourist.setId(id);
        touristService.save(tourist);
        redirectAttributes.addFlashAttribute("success", "Данные туриста успешно обновлены");
        return "redirect:/tourists";
    }
}
