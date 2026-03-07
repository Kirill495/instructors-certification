package org.tourism.instructors.domain.tourist.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.tourism.instructors.domain.tourist.model.contactinfo.ContactInfoItem;

import java.util.List;

@Repository
public interface ContactInfoRepository extends JpaRepository<ContactInfoItem, Integer> {

    List<ContactInfoItem> findAllByTouristId (Integer touristId);

}
