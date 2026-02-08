package org.tourism.instructors.domain.catalog.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.tourism.instructors.domain.catalog.model.KindOfTourism;

import java.util.List;

@Repository
public interface KindOfTourismRepository extends JpaRepository<KindOfTourism, Integer> {

    List<KindOfTourism> findByInactiveFalseOrderByIdAsc ();
    List<KindOfTourism> findAllByOrderByIdAsc ();
}
