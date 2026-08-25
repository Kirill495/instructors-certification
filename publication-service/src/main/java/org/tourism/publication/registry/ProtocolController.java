package org.tourism.publication.registry;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.tourism.publication.registry.dto.ProtocolResponse;

@RestController
@RequestMapping("/api/v1/protocols")
@RequiredArgsConstructor
public class ProtocolController {

    private final ProtocolRegistry protocolRegistry;

    @GetMapping("/{number}")
    public Optional<ProtocolResponse> getProtocolByNumber(
            @PathVariable("number") String protocolNumber) {
        return protocolRegistry.findProtocolByNumber(protocolNumber);
    }
}
