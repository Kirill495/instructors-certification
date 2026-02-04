package org.tourism.instructors.domain.tourist;

import org.tourism.instructors.domain.tourist.dto.TouristDTO;

import java.util.List;

public interface TouristService {

    List<TouristDTO> getAllTourists ();
}
