package org.tourism.instructors.application.tourist.impl;

import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.tourism.instructors.api.protocol.mapper.GradeAssignmentMapper;
import org.tourism.instructors.api.tourist.dto.TouristDTO;
import org.tourism.instructors.api.tourist.dto.TouristLightDTO;
import org.tourism.instructors.api.tourist.mapper.TouristMapper;
import org.tourism.instructors.application.tourist.TouristService;
import org.tourism.instructors.domain.protocol.repository.ProtocolRepository;
import org.tourism.instructors.domain.tourist.TouristRepository;
import org.tourism.instructors.domain.tourist.model.Tourist;

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
    private final GradeAssignmentMapper gradeAssignmentMapper;

    public TouristServiceImpl (TouristRepository touristRepository, ProtocolRepository protocolRepository, TouristMapper touristMapper, GradeAssignmentMapper gradeAssignmentMapper) {
        this.touristRepository = touristRepository;
        this.protocolRepository = protocolRepository;
        this.touristMapper = touristMapper;
        this.gradeAssignmentMapper = gradeAssignmentMapper;
    }

    @Override
    public int countTourists () {
        return (int) touristRepository.count();
    }

    @Override
    public List<TouristDTO> getAllTourists () {
        List<Tourist> tourists = touristRepository.findAll();
        Map<Integer, List<ProtocolRepository.GradeAssignmentProjection>> assignments = getAssignments(tourists);
        return tourists.stream().map(tourist -> touristMapper.toDTO(tourist, getTouristAssignments(tourist, assignments))).toList();
    }

    @Override
    public List<TouristLightDTO> searchLightTourists (String query) {
        return searchTouristsInner(query).stream().map(touristMapper::toLightDTO).toList();
    }

    @Override
    public List<TouristDTO> searchTourists (String query) {

        List<Tourist> tourists = searchTouristsInner(query);
        Map<Integer, List<ProtocolRepository.GradeAssignmentProjection>> assignments = getAssignments(tourists);
        return tourists.stream().map(tourist -> touristMapper.toDTO(tourist, getTouristAssignments(tourist, assignments))).toList();
    }

    private static @NonNull List<ProtocolRepository.GradeAssignmentProjection> getTouristAssignments (Tourist tourist, Map<Integer, List<ProtocolRepository.GradeAssignmentProjection>> assignments) {
        return assignments.getOrDefault(tourist.getId(), Collections.emptyList()).stream().sorted(Comparator.comparing(ProtocolRepository.GradeAssignmentProjection::getProtocolDate)).toList();
    }

    private @NonNull Map<Integer, List<ProtocolRepository.GradeAssignmentProjection>> getAssignments (List<Tourist> tourists) {
        Map<Integer, List<ProtocolRepository.GradeAssignmentProjection>> assignments = protocolRepository
            .getAssignments(tourists.stream().map(Tourist::getId).toList())
            .stream()
            .collect(Collectors.groupingBy(ProtocolRepository.GradeAssignmentProjection::getTouristId));
        return assignments;
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
        Tourist tourist = touristRepository.findById(id).orElseThrow(() -> new RuntimeException("турист не найден"));
        List<ProtocolRepository.GradeAssignmentProjection> assignments = protocolRepository.getAssignments(List.of(id));
        TouristDTO touristInfo = touristMapper.toDTO(tourist, assignments);
        return touristInfo;
    }

    private boolean containsOnlyDigits (String str) {
        return str.matches("^\\d+$");
    }

}
