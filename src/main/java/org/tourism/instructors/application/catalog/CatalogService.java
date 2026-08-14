package org.tourism.instructors.application.catalog;

import java.util.List;
import org.tourism.instructors.api.catalog.dto.GradeDTO;
import org.tourism.instructors.api.catalog.dto.KindOfTourismDTO;
import org.tourism.instructors.api.catalog.dto.KindOfTourismListDTO;

public interface CatalogService {
    int countActiveKindsOfTourism();

    List<KindOfTourismListDTO> findAllKindsOfTourism();

    List<KindOfTourismListDTO> findActiveKindsOfTourism();

    KindOfTourismDTO getKindOfTourismById(int id);

    void saveKindOfTourism(KindOfTourismDTO dto);

    void deleteKindOfTourism(int id);

    int countActiveGrades();

    List<GradeDTO> findAllGrades();

    List<GradeDTO> findActiveGrades();

    GradeDTO findGradeById(int id);

    void saveGrade(GradeDTO gradeDTO);

    void deleteGrade(int id);
}
