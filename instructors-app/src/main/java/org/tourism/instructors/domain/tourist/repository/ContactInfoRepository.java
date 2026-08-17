package org.tourism.instructors.domain.tourist.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.tourism.instructors.domain.tourist.model.contactinfo.ContactInfoItem;
import org.tourism.instructors.domain.tourist.model.contactinfo.ContactInfoType;

@Repository
public interface ContactInfoRepository extends JpaRepository<ContactInfoItem, Integer> {

    List<ContactInfoItem> findAllByTouristId(Integer touristId);

    @Query(
            "Select ci FROM ContactInfoItem as ci join fetch ci.tourist where ci.type = :type and ci.value = :value")
    Optional<ContactInfoItem> findTouristIdByTypeAndValue(
            @Param("type") ContactInfoType type, @Param("value") String value);
}
