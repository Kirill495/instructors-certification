package org.tourism.instructors.application.tourist.impl;

import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.tourism.instructors.api.tourist.dto.TouristDTO;
import org.tourism.instructors.api.tourist.dto.TouristLightDTO;
import org.tourism.instructors.api.tourist.mapper.TouristMapper;
import org.tourism.instructors.application.tourist.TouristService;
import org.tourism.instructors.domain.protocol.repository.ProtocolRepository;
import org.tourism.instructors.domain.tourist.model.Tourist;
import org.tourism.instructors.domain.tourist.repository.TouristRepository;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TouristServiceImpl implements TouristService {

    private final TouristRepository touristRepository;
    private final ProtocolRepository protocolRepository;
    private final TouristMapper touristMapper;

    public TouristServiceImpl (TouristRepository touristRepository,
                               ProtocolRepository protocolRepository,
                               TouristMapper touristMapper) {
        this.touristRepository = touristRepository;
        this.protocolRepository = protocolRepository;
        this.touristMapper = touristMapper;
    }

    @Override
    public int countTourists () {
        return (int) touristRepository.count();
    }

    @Override
    public Page<TouristDTO> getAllTourists (Pageable pageable) {
        Page<Tourist> touristsPage = touristRepository.findAll(pageable);
        Map<Integer, List<ProtocolRepository.GradeAssignmentProjection>> assignments = getAssignments(touristsPage.getContent());
        List<TouristDTO> touristDTOs = touristsPage.stream().map(tourist -> touristMapper.toDTO(tourist, getTouristAssignments(tourist, assignments))).toList();
        return new PageImpl<>(touristDTOs, pageable, touristsPage.getTotalElements());
    }

    @Override
    public List<TouristLightDTO> searchLightTourists (String query) {
        return searchTouristsInner(query).stream().map(touristMapper::toLightDTO).toList();
    }

    @Override
    public Page<TouristDTO> searchTourists (String query, Pageable pageable) {

        List<Tourist> tourists = searchTouristsInner(query);
        Map<Integer, List<ProtocolRepository.GradeAssignmentProjection>> assignments = getAssignments(tourists);
        List<TouristDTO> touristDTOs = tourists.stream().map(tourist -> touristMapper.toDTO(tourist, getTouristAssignments(tourist, assignments))).toList();
        return new PageImpl<>(touristDTOs, pageable, touristDTOs.size());
    }

    @Override
    public void delete (int touristId) {
        touristRepository.deleteById(touristId);
    }

    @Override
    public void save (TouristDTO tourist) {
        touristRepository.save(touristMapper.toEntity(tourist));
    }

    @Override
    public TouristDTO findTouristById (int id) {
        Tourist tourist = touristRepository.findById(id).orElseThrow(() -> new RuntimeException("турист не найден"));
        List<ProtocolRepository.GradeAssignmentProjection> assignments = protocolRepository.getAssignments(List.of(id));
        return touristMapper.toDTO(tourist, assignments);
    }

    private static @NonNull List<ProtocolRepository.GradeAssignmentProjection> getTouristAssignments (Tourist tourist, Map<Integer, List<ProtocolRepository.GradeAssignmentProjection>> assignments) {
        return assignments.getOrDefault(tourist.getId(), Collections.emptyList()).stream().sorted(Comparator.comparing(ProtocolRepository.GradeAssignmentProjection::getProtocolDate)).toList();
    }

    private @NonNull Map<Integer, List<ProtocolRepository.GradeAssignmentProjection>> getAssignments (List<Tourist> tourists) {
        return protocolRepository
            .getAssignments(tourists.stream().map(Tourist::getId).toList())
            .stream()
            .collect(Collectors.groupingBy(ProtocolRepository.GradeAssignmentProjection::getTouristId));
    }

    private List<Tourist> searchTouristsInner (String query) {
        if (containsOnlyDigits(query)) {
            return touristRepository.searchByCertificationId(query);
        } else {
            return touristRepository.searchByLastNameStartingWithIgnoreCase(query);
        }
    }

    private boolean containsOnlyDigits (String str) {
        return str.matches("^\\d+$");
    }

}
