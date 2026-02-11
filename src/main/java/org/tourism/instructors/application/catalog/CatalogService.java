package org.tourism.instructors.application.catalog;

import org.tourism.instructors.api.catalog.dto.GradeDTO;
import org.tourism.instructors.api.catalog.dto.KindOfTourismDTO;
import org.tourism.instructors.api.catalog.dto.KindOfTourismListDTO;

import java.util.List;

public interface CatalogService {
    int countActiveKindsOfTourism ();
    List<KindOfTourismListDTO> findAllKindsOfTourism ();
    List<KindOfTourismListDTO> findActiveKindsOfTourism ();
    KindOfTourismDTO getKindOfTourismById (int id);
    void saveKindOfTourism (KindOfTourismDTO dto);
    void deleteKindOfTourism (int id);

    int countActiveGrades();
    List<GradeDTO> findAllGrades();
    List<GradeDTO> findActiveGrades();
    GradeDTO findGradeById(int id);
    void saveGrade (GradeDTO gradeDTO);
    void deleteGrade (int id);
}
