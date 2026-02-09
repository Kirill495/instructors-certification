package org.tourism.instructors.api.protocol;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.tourism.instructors.application.protocol.ProtocolService;

@Controller
@RequestMapping("/protocols")
public class ProtocolController {

    private final ProtocolService protocolService;

    public ProtocolController (ProtocolService protocolService) {
        this.protocolService = protocolService;
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
}
