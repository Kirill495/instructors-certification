package org.tourism.instructors.domain.tourist;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.tourism.instructors.api.tourist.dto.TouristLightDTO;
import org.tourism.instructors.domain.tourist.model.Tourist;

import java.util.Arrays;
import java.util.List;

@Repository
public interface TouristRepository extends JpaRepository<Tourist, Integer> {
    List<Tourist> searchByLastNameStartingWithIgnoreCase (String lastName);

    List<Tourist> searchByCertificationId (String certificationId);
}
