package org.tourism.instructors.api.tourist;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.tourism.instructors.api.tourist.dto.TouristDTO;
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

    @GetMapping
    public Page<TouristDTO> listTourists(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "certificationId") String sort,
            @RequestParam(defaultValue = "asc") String order,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Sort sortObj = Sort.by("desc".equals(order) ? Sort.Direction.DESC : Sort.Direction.ASC, sort);
        Pageable pageable = PageRequest.of(page, size, sortObj);

        return search != null && !search.trim().isEmpty()
                       ? touristService.searchTourists(search, pageable)
                       : touristService.getAllTourists(pageable);
    }
}
