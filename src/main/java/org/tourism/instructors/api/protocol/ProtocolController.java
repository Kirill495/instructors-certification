package org.tourism.instructors.api.protocol;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.tourism.instructors.api.protocol.dto.ProtocolDTO;
import org.tourism.instructors.api.protocol.dto.ProtocolForListDTO;
import org.tourism.instructors.application.catalog.CatalogService;
import org.tourism.instructors.application.protocol.ProtocolService;

@Controller
@RequestMapping("/protocols")
public class ProtocolController {

    private final ProtocolService protocolService;
    private final CatalogService catalogService;


    public ProtocolController (ProtocolService protocolService, CatalogService catalogService) {
        this.protocolService = protocolService;
        this.catalogService = catalogService;
    }

    @GetMapping
    public String listProtocols(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false, defaultValue = "asc") String order,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size,
            @RequestParam(required = false) Integer highlightId,
            @RequestHeader(value = "X-Requested-With", required = false) String requestedWith,
            Model model) {

        Sort sortObj = createSort(sort, order);
        Pageable pageable;
        if (highlightId != null && page == 0) {
            int rowIndex = protocolService.getProtocolIndex(highlightId);
            if (rowIndex >= 0) {
                int targetPage = rowIndex / size;
                int expandedSize = size * (targetPage + 1);
                pageable = PageRequest.of(0, expandedSize, sortObj);
            } else {
                pageable = PageRequest.of(page, size, sortObj);
            }
        } else {
            pageable = PageRequest.of(page, size, sortObj);
        }
        Page<ProtocolForListDTO> pageProtocol;
        pageProtocol = protocolService.getProtocolsForList(search, pageable);

        addPaginationAttributes(model, pageProtocol, search, page, size, sort, order);
        if ("XMLHttpRequest".equals(requestedWith)) {
            return "protocols/list :: tableRows";
        }
        return "protocols/list";
    }

    private void addPaginationAttributes(Model model, Page<ProtocolForListDTO> page, String searchQuery, int currentPage, int size, String sort, String order) {

        model.addAttribute("protocols", page.getContent());
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("totalPages", page.getTotalPages());
        model.addAttribute("totalElements", page.getTotalElements());
        model.addAttribute("pageSize", size);
        model.addAttribute("sortField", sort);
        model.addAttribute("sortOrder", order);
        model.addAttribute("searchQuery", searchQuery);

        int totalPages = page.getTotalPages();
        int pageStart = Math.max(0, currentPage - 4);
        int pageEnd = Math.min(totalPages - 1, currentPage + 5);

        if (pageEnd - pageStart < 9) {
            if (pageStart == 0) {
                pageEnd = Math.min(9, totalPages - 1);
            } else if (pageEnd == totalPages - 1) {
                pageStart = Math.max(0, totalPages - 10);
            }
        }

        model.addAttribute("pageStart", pageStart);
        model.addAttribute("pageEnd", pageEnd);

    }

    private static Sort createSort (String sort, String order) {
        Sort sortObj = Sort.unsorted();
        if (sort != null) {
            Sort.Direction direction = "desc".equals(order) ? Sort.Direction.DESC : Sort.Direction.ASC;
            sortObj = Sort.by(direction, sort);
        } else {
            sortObj = Sort.by(Sort.Direction.ASC, "number");
        }
        return sortObj;
    }

    @GetMapping("/{id}")
    public String viewProtocol(@PathVariable int id, Model model) {
        model.addAttribute("protocol", protocolService.getProtocolById(id));
        return "protocols/view";
    }

    @GetMapping("/{id}/edit")
    public String editProtocol(@PathVariable int id, Model model) {
        model.addAttribute("protocol", protocolService.getProtocolById(id));
        model.addAttribute("grades", catalogService.findActiveGrades());
        model.addAttribute("kindsOfTourism", catalogService.findActiveKindsOfTourism());
        return "protocols/edit";
    }

    @GetMapping("/new")
    public String newProtocol(Model model) {
        model.addAttribute("protocol", new ProtocolDTO());
        model.addAttribute("grades", catalogService.findActiveGrades());
        model.addAttribute("kindsOfTourism", catalogService.findActiveKindsOfTourism());
        return "protocols/edit";
    }

    @PostMapping("/save")
    public String saveProtocol(@ModelAttribute ProtocolDTO protocol,
                               RedirectAttributes redirectAttributes) {
        protocolService.saveProtocol(protocol);
        redirectAttributes.addFlashAttribute("successMessage", "Протокол записан");
        return "redirect:/protocols";
    }
}
