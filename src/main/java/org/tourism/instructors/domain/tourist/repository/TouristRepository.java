package org.tourism.instructors.domain.tourist.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.tourism.instructors.domain.tourist.model.Tourist;

import java.util.List;

@Repository
public interface TouristRepository extends JpaRepository<Tourist, Integer> {
    List<Tourist> searchByLastNameStartingWithIgnoreCase (String lastName);

    List<Tourist> searchByCertificationId (String certificationId);

//    Page<Tourist> findAllTourists (Pageable pageable);


}
