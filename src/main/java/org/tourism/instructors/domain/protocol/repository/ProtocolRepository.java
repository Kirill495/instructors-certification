package org.tourism.instructors.domain.protocol.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.tourism.instructors.domain.protocol.Protocol;

import java.util.List;

@Repository
public interface ProtocolRepository extends JpaRepository<Protocol, Integer> {

    @Query("SELECT DISTINCT p.number FROM Protocol p")
    Page<Integer> findAllProtocols(Pageable pageable);

    @Query("SELECT DISTINCT p.number " +
                   "FROM Protocol p LEFT JOIN p.protocolContents c LEFT JOIN c.tourist " +
                   "WHERE c.tourist.lastName ILIKE CONCAT(:search, '%')" +
                   "OR c.tourist.firstName ILIKE CONCAT(:search, '%')")
    Page<Integer> searchByTouristLastNameStartingWithIgnoreCase (@Param("search") String search, Pageable pageable);

    @Query("SELECT p FROM Protocol p " +
           "LEFT JOIN FETCH p.protocolContents c " +
           "LEFT JOIN FETCH c.tourist " +
           "WHERE p.number IN :numbers")
    List<Protocol> getProtocolWithContentByIDs(@Param("numbers") List<Integer> numbers, Sort sort);
}
