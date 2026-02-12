package org.tourism.instructors.domain.tourist.impl;

import org.springframework.stereotype.Service;
import org.tourism.instructors.api.tourist.dto.TouristLightDTO;
import org.tourism.instructors.api.tourist.mapper.TouristMapper;
import org.tourism.instructors.domain.tourist.TouristRepository;
import org.tourism.instructors.domain.tourist.TouristService;
import org.tourism.instructors.api.tourist.dto.TouristDTO;
import org.tourism.instructors.domain.tourist.model.Tourist;

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
    public List<TouristLightDTO> searchLightTourists (String query) {
        return searchTouristsInner(query).stream().map(touristMapper::toLightDTO).toList();
    }

    @Override
    public List<TouristDTO> searchTourists (String query) {
        return searchTouristsInner(query).stream().map(touristMapper::toDTO).toList();
    }

    private List<Tourist> searchTouristsInner (String query) {
        if (containsOnlyDigits(query)) {
            return touristRepository.searchByCertificationId(query);
        } else {
            return touristRepository.searchByLastNameStartingWithIgnoreCase(query);
        }
    }

    @Override
    public void save (TouristDTO tourist) {
        touristRepository.save(touristMapper.toEntity(tourist));
    }

    @Override
    public TouristDTO findTouristById (int id) {
        return touristRepository.findById(id).map(touristMapper::toDTO).orElseThrow(() -> new RuntimeException("турист не найден"));
    }

    private boolean containsOnlyDigits (String str) {
        return str.matches("^\\d+$");
    }

}
