package org.tourism.instructors.application.catalog.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tourism.instructors.api.catalog.dto.GradeDTO;
import org.tourism.instructors.api.catalog.dto.KindOfTourismDTO;
import org.tourism.instructors.api.catalog.dto.KindOfTourismListDTO;
import org.tourism.instructors.api.catalog.mapper.GradeMapper;
import org.tourism.instructors.api.catalog.mapper.KindOfTourismMapper;
import org.tourism.instructors.application.catalog.CatalogService;
import org.tourism.instructors.domain.catalog.repository.GradeRepository;
import org.tourism.instructors.domain.catalog.repository.KindOfTourismRepository;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class CatalogServiceImpl implements CatalogService {

    private final KindOfTourismRepository kindOfTourismRepository;
    private final GradeRepository gradeRepository;
    private final KindOfTourismMapper kindOfTourismMapper;
    private final GradeMapper gradeMapper;


    public CatalogServiceImpl (KindOfTourismRepository kindOfTourismRepository, GradeRepository gradeRepository, KindOfTourismMapper kindOfTourismMapper, GradeMapper gradeMapper) {
        this.kindOfTourismRepository = kindOfTourismRepository;
        this.gradeRepository = gradeRepository;
        this.kindOfTourismMapper = kindOfTourismMapper;
        this.gradeMapper = gradeMapper;
    }

    @Override
    public int countActiveKindsOfTourism () {
        return (int) kindOfTourismRepository.countKindOfTourismByInactive(false);
    }

    @Override
    public List<KindOfTourismListDTO> findAllKindsOfTourism () {
        return kindOfTourismRepository.findAllByOrderByIdAsc()
                       .stream()
                       .map(kindOfTourismMapper::toListDTO)
                       .toList();

    }

    @Override
    public List<KindOfTourismListDTO> findActiveKindsOfTourism () {
        return kindOfTourismRepository.findByInactiveFalseOrderByIdAsc()
                       .stream()
                       .map(kindOfTourismMapper::toListDTO)
                       .toList();
    }

    @Override
    public KindOfTourismDTO getKindOfTourismById (int id) {
        return kindOfTourismRepository.findById(id)
                       .map(kindOfTourismMapper::toDTO)
                       .orElseThrow(() -> new RuntimeException("Вид туризма c ID:" + id + " не найден"));
    }

    @Override
    @Transactional
    public void saveKindOfTourism (KindOfTourismDTO inputDTO) {
        kindOfTourismRepository.save(kindOfTourismMapper.toEntity(inputDTO));
    }

    @Override
    @Transactional
    public void deleteKindOfTourism (int id) {
        kindOfTourismRepository.deleteById(id);
    }

    @Override
    public int countActiveGrades () {
        return gradeRepository.countGradesByInactive(false);
    }

    @Override
    public List<GradeDTO> findAllGrades () {
        return gradeRepository.findAllByOrderById().stream()
                       .map(gradeMapper::toDTO)
                       .toList();
    }

    @Override
    public List<GradeDTO> findActiveGrades () {
        return gradeRepository.findByInactiveFalseOrderById().stream()
                       .map(gradeMapper::toDTO)
                       .toList();
    }

    @Override
    public GradeDTO findGradeById (int id) {
        return gradeRepository.findById(id)
                       .map(gradeMapper::toDTO)
                       .orElseThrow(() -> new RuntimeException("Звание с ID:" + id + " не найдено"));
    }

    @Override
    @Transactional
    public void saveGrade (GradeDTO gradeDTO) {
        gradeRepository.save(gradeMapper.toEntity(gradeDTO));
    }

    @Override
    @Transactional
    public void deleteGrade (int id) {
        gradeRepository.deleteById(id);
    }
}
