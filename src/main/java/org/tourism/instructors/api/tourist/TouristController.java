package org.tourism.instructors.api.tourist;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.tourism.instructors.api.tourist.dto.TouristDTO;
import org.tourism.instructors.application.tourist.TouristService;

@Controller
@RequestMapping("/tourists")
public class TouristController {

    private final TouristService touristService;

    public TouristController (TouristService touristService) {
        this.touristService = touristService;
    }

    @GetMapping
    public String listTourists(@RequestParam(required = false) String search,
                               @RequestParam(required = false, defaultValue = "certificationId") String sort,
                               @RequestParam(required = false, defaultValue = "asc") String order,
                               @RequestParam(required = false, defaultValue = "0") int page,
                               @RequestParam(required = false, defaultValue = "20") int size,
                               @RequestHeader(value = "X-Requested-With", required = false) String requestedWith,
                               Model model) {
        Sort sortObj = createSort(sort, order);
        Pageable pageable = PageRequest.of(page, size, sortObj);
        Page<TouristDTO> touristsPage;
        if (search != null && !search.trim().isEmpty()) {
            touristsPage = touristService.searchTourists(search, pageable);
        } else {
            touristsPage = touristService.getAllTourists(pageable);
        }

        addPaginationAttributes(model, touristsPage, search, page, size, sort, order);
        if ("XMLHttpRequest".equals(requestedWith)) {
            return "tourists/list :: tableRows";
        }
        return "tourists/list";
    }

    private static void addPaginationAttributes (Model model, Page<TouristDTO> touristsPage, String search, int currentPage, int size, String sort, String order) {
        model.addAttribute("tourists", touristsPage.getContent());

        model.addAttribute("search", search);
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("totalPages", touristsPage.getTotalPages());
        model.addAttribute("totalElements", touristsPage.getTotalElements());
        model.addAttribute("pageSize", size);
        model.addAttribute("sortField", sort);
        model.addAttribute("sortOrder", order);
        model.addAttribute("searchQuery", search);
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

    private static Sort createSort(String sort, String order) {
        Sort sortObj = Sort.unsorted();
        if (sort != null) {
            Sort.Direction direction = "desc".equals(order) ? Sort.Direction.DESC : Sort.Direction.ASC;
            sortObj = Sort.by(direction, sort);
        }
        return sortObj;
    }

}
