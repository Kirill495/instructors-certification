package org.tourism.publication.registry;

import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.tourism.publication.registry.dto.ProtocolResponse;

@RestController
@RequestMapping("/api/v1/protocols")
@RequiredArgsConstructor
public class ProtocolController {

    private final ProtocolRegistry protocolRegistry;

    @GetMapping("/{number}")
    public ProtocolResponse getProtocolByNumber(@PathVariable("number") String protocolNumber) {
        return protocolRegistry
                .findProtocolByNumber(protocolNumber)
                .orElseThrow(
                        () ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND, "Протокол не найден"));
    }

    @GetMapping
    public List<ProtocolResponse> getProtocolsInPeriod(
            @RequestParam("since") LocalDate since, @RequestParam("till") LocalDate till) {

        if (!since.isBefore(till)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "since должен быть раньше till");
        }

        return protocolRegistry.findProtocolsInPeriod(since, till);
    }
}
