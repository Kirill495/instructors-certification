package org.tourism.instructors.domain.catalog.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.tourism.instructors.domain.catalog.model.Grade;

@Repository
public interface GradeRepository extends JpaRepository<Grade, Integer> {
    List<Grade> findAllByOrderById();

    List<Grade> findByInactiveFalseOrderById();

    int countGradesByInactive(boolean isInactive);
}
