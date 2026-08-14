package org.tourism.instructors.domain.catalog.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.tourism.instructors.domain.catalog.model.KindOfTourism;

@Repository
public interface KindOfTourismRepository extends JpaRepository<KindOfTourism, Integer> {

    long countKindOfTourismByInactive(boolean isInactive);

    List<KindOfTourism> findByInactiveFalseOrderByIdAsc();

    List<KindOfTourism> findAllByOrderByIdAsc();
}
