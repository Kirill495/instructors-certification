package org.tourism.instructors.domain.protocol.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.tourism.instructors.domain.protocol.Protocol;

import java.util.List;

@Repository
public interface ProtocolRepository extends JpaRepository<Protocol, Integer> {

    @Query("SELECT p FROM Protocol p JOIN FETCH p.protocolContents c JOIN FETCH c.tourist")
    List<Protocol> findAllProtocolsWithContent();

    @Query("SELECT p " +
                   "FROM Protocol p JOIN FETCH p.protocolContents c JOIN FETCH c.tourist " +
                   "WHERE c.tourist.lastName ILIKE CONCAT(:search, '%')" +
                   "OR c.tourist.firstName ILIKE CONCAT(:search, '%')")
    List<Protocol> searchByTouristLastNameStartingWithIgnoreCase (@Param("search") String search);
}
