package org.tourism.instructors.domain.tourist.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.tourism.instructors.domain.tourist.model.Tourist;

@Repository
public interface TouristRepository extends JpaRepository<Tourist, Integer> {
    List<Tourist> searchByLastNameStartingWithIgnoreCase(String lastName);

    List<Tourist> searchByCertificationId(String certificationId);

    List<Tourist>
            searchByLastNameStartingWithIgnoreCaseAndFirstNameStartingWithIgnoreCaseAndMiddleNameStartingWithIgnoreCase(
                    String lastName, String firstName, String middleName);

    List<Tourist> searchByLastNameStartingWithIgnoreCaseAndFirstNameStartingWithIgnoreCase(
            String lastName, String firstName);
}
