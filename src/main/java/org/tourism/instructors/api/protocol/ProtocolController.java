package org.tourism.instructors.api.protocol;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.tourism.instructors.api.protocol.dto.ProtocolDTO;
import org.tourism.instructors.application.protocol.ProtocolService;
import org.tourism.instructors.application.catalog.CatalogService;

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
    public String listProtocols(@RequestParam(value = "search", required = false) String search, Model model) {
        if (search != null && !search.trim().isEmpty()) {
            model.addAttribute("protocols", protocolService.searchProtocols(search));
        } else {
            model.addAttribute("protocols", protocolService.getProtocolsForList());
        }
        model.addAttribute("search", search);
        return "protocols/list";
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
