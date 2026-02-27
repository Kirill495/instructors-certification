package org.tourism.instructors.application.tourist;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.tourism.instructors.api.tourist.dto.TouristDTO;
import org.tourism.instructors.api.tourist.dto.TouristLightDTO;

import java.util.List;

public interface TouristService {

    int countTourists();
    Page<TouristDTO> getAllTourists (Pageable pageable);
    List<TouristLightDTO> searchLightTourists (String query);
    Page<TouristDTO> searchTourists (String query, Pageable pageable);
    TouristDTO findTouristById(int id);
    void save(TouristDTO tourist);
    void delete (int touristId);
}
