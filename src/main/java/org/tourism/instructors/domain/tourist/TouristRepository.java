package org.tourism.instructors.domain.tourist;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.tourism.instructors.domain.tourist.model.Tourist;

@Repository
public interface TouristRepository extends JpaRepository<Tourist, Integer> {
}
