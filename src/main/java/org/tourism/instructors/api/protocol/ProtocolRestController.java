package org.tourism.instructors.api.protocol;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.tourism.instructors.api.protocol.dto.ProtocolLightDTO;
import org.tourism.instructors.application.protocol.ProtocolService;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/protocols")
public class ProtocolRestController {

    private final ProtocolService protocolService;

    public ProtocolRestController(ProtocolService protocolService) {
        this.protocolService = protocolService;
    }

    @GetMapping("/search")
    public List<ProtocolLightDTO> search(@RequestParam(defaultValue = "") String query) {
        if (query.trim().isEmpty()){
            return protocolService.getLastDrafts();
        } else {
            return protocolService.searchDraftsByNumber(query);
        }
    }
}