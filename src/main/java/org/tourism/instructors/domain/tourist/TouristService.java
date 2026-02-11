package org.tourism.instructors.domain.tourist;

import org.tourism.instructors.api.tourist.dto.TouristLightDTO;
import org.tourism.instructors.api.tourist.dto.TouristDTO;

import java.util.List;

public interface TouristService {

    int countTourists();
    List<TouristDTO> getAllTourists ();
    List<TouristLightDTO> searchTourists (String query);
    TouristDTO findTouristById(int id);
    void save(TouristDTO tourist);
}
