package org.tourism.instructors.domain.catalog.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.tourism.instructors.domain.catalog.model.Grade;

import java.util.List;

@Repository
public interface GradeRepository extends JpaRepository<Grade, Integer> {
    List<Grade> findAllByOrderById();

    List<Grade> findByInactiveFalseOrderById ();
}
