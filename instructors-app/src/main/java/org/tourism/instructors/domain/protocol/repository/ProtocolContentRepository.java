package org.tourism.instructors.domain.protocol.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.tourism.instructors.domain.catalog.model.Grade;
import org.tourism.instructors.domain.catalog.model.KindOfTourism;
import org.tourism.instructors.domain.protocol.ProtocolContent;

@Repository
public interface ProtocolContentRepository extends JpaRepository<ProtocolContent, Integer> {

    boolean existsProtocolContentByGrade(Grade grade);

    boolean existsByKindOfTourism(KindOfTourism kindOfTourism);
}
