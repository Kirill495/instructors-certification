package org.tourism.instructors.api.tourist;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.tourism.instructors.api.tourist.dto.TouristLightDTO;
import org.tourism.instructors.application.tourist.TouristService;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/tourists")
public class TouristRestController {
    private final TouristService touristService;

    public TouristRestController (TouristService touristService) {
        this.touristService = touristService;
    }

    @GetMapping("/search")
    public List<TouristLightDTO> searchTourists(@RequestParam(required = false, defaultValue = "") String query) {
        if (query.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return touristService.searchLightTourists(query);
    }
}
