package org.tourism.instructors.api.protocol;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
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
    public String listProtocols(Model model) {
        model.addAttribute("protocols", protocolService.getProtocols());
        return "protocols/list";
    }

    @GetMapping("/{id}")
    public String viewProtocol(@PathVariable int id, Model model) {
        model.addAttribute("protocol", protocolService.getProtocolById(id));
        return "protocols/view";
    }
    @GetMapping("/new")
    public String newProtocol(Model model) {
        model.addAttribute("protocol", new ProtocolDTO());
        model.addAttribute("grades", catalogService.findActiveGrades());
        model.addAttribute("kindsOfTourism", catalogService.findActiveKindsOfTourism());
        return "protocols/edit";
    }
}
