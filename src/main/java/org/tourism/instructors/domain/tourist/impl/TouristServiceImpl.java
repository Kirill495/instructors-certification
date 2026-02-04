package org.tourism.instructors.domain.tourist.impl;

import org.springframework.stereotype.Service;
import org.tourism.instructors.domain.tourist.TouristRepository;
import org.tourism.instructors.domain.tourist.TouristService;
import org.tourism.instructors.domain.tourist.dto.TouristDTO;

import java.util.List;

@Service
public class TouristServiceImpl implements TouristService {

    private final TouristRepository touristRepository;

    public TouristServiceImpl (TouristRepository touristRepository) {
        this.touristRepository = touristRepository;
    }

    @Override
    public List<TouristDTO> getAllTourists () {
        return touristRepository.findAll().stream()
                       .map(tourist -> new TouristDTO(tourist.getId(), tourist.getLastName()))
                       .toList();
    }
}
