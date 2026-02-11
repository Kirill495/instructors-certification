package org.tourism.instructors.domain.tourist.impl;

import org.springframework.stereotype.Service;
import org.tourism.instructors.api.tourist.dto.TouristLightDTO;
import org.tourism.instructors.api.tourist.mapper.TouristMapper;
import org.tourism.instructors.domain.tourist.TouristRepository;
import org.tourism.instructors.domain.tourist.TouristService;
import org.tourism.instructors.domain.tourist.dto.TouristDTO;

import java.util.List;

@Service
public class TouristServiceImpl implements TouristService {

    private final TouristRepository touristRepository;
    private final TouristMapper touristMapper;

    public TouristServiceImpl (TouristRepository touristRepository, TouristMapper touristMapper) {
        this.touristRepository = touristRepository;
        this.touristMapper = touristMapper;
    }

    @Override
    public int countTourists () {
        return (int) touristRepository.count();
    }

    @Override
    public List<TouristDTO> getAllTourists () {
        return touristRepository.findAll().stream().map(touristMapper::toDTO).toList();
    }

    @Override
    public List<TouristLightDTO> searchTourists (String query) {
        if (containsOnlyDigits(query)) {
            return touristRepository.searchByCertificationId(query).stream().map(touristMapper::toLightDTO).toList();
        } else {
            return touristRepository.searchByLastNameStartingWithIgnoreCase(query).stream().map(touristMapper::toLightDTO).toList();
        }
    }

    private boolean containsOnlyDigits (String str) {
        return str.matches("^\\d+$");
    }

}
