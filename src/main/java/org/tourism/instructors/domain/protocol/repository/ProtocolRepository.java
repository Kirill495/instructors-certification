package org.tourism.instructors.domain.protocol.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.tourism.instructors.domain.catalog.model.Grade;
import org.tourism.instructors.domain.catalog.model.KindOfTourism;
import org.tourism.instructors.domain.protocol.Protocol;
import org.tourism.instructors.domain.protocol.ProtocolStatus;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ProtocolRepository extends JpaRepository<Protocol, Integer> {

    @Query("SELECT p.date as assignmentDate, " +
                   "t.firstName as firstName, " +
                   "t.lastName as lastName, " +
                   "t.middleName as middleName, " +
                   "c.club as club, " +
                   "g.title as gradeTitle, " +
                   "COALESCE(g.expiresInYears, 0) as expiresInYears, " +
                   "COALESCE(k.title, '') as kindOfTourismTitle " +
           "FROM " +
               "Protocol p " +
               "LEFT JOIN p.protocolContents AS c " +
               "LEFT JOIN c.tourist AS t " +
               "LEFT JOIN c.grade as g " +
               "LEFT JOIN c.kindOfTourism as k " +
           "WHERE p.date between :dateFrom AND :dateTill " +
           "ORDER BY assignmentDate")
    List<ReportAssignmentProjection> getAssignmentsForReport(
            @Param("dateFrom") LocalDate dateFrom,
            @Param("dateTill") LocalDate dateTill);

    @Query("SELECT DISTINCT p.id as id, p.number as number, p.date as date FROM Protocol p")
    Page<ProtocolProjection> findAllProtocols(Pageable pageable);

    @Query("SELECT DISTINCT p.id as id, p.number as number, p.date as date " +
                   "FROM Protocol p LEFT JOIN p.protocolContents c LEFT JOIN c.tourist " +
                   "WHERE c.tourist.lastName ILIKE CONCAT(:search, '%')" +
                   "OR c.tourist.firstName ILIKE CONCAT(:search, '%')")
    Page<ProtocolProjection> searchByTouristLastNameStartingWithIgnoreCase (@Param("search") String search, Pageable pageable);

    @Query("SELECT p FROM Protocol p " +
           "LEFT JOIN FETCH p.protocolContents c " +
           "LEFT JOIN FETCH c.tourist " +
           "WHERE p.id IN :ids")
    List<Protocol> getProtocolWithContentByIDs(@Param("ids") List<Integer> ids, Sort sort);

    @Query("SELECT count(*) FROM Protocol p WHERE p.id < :protocolId")
    int countOfRowsBefore(@Param("protocolId") int protocolId);

    @Query("SELECT p.id as id, p.number as number, p.date as date FROM Protocol p WHERE p.number ILIKE CONCAT(:query, '%') AND p.status = :status ORDER BY p.number ASC")
    List<ProtocolProjection> searchByNumberAndStatus(@Param("query") String query, @Param("status") ProtocolStatus status);


    @Query("SELECT p.id as protocolId, " +
                   "p.date as protocolDate, " +
                   "t.id as touristId, " +
                   "c.grade as grade, " +
                   "c.kindOfTourism as kindOfTourism " +
                   "FROM Protocol p " +
                   "LEFT JOIN  p.protocolContents c " +
                   "LEFT JOIN c.grade g " +
                   "LEFT JOIN c.kindOfTourism k " +
                   "LEFT JOIN  c.tourist t " +
               "WHERE c.tourist.id in (:tourist_ids)")
    List<GradeAssignmentProjection> getAssignments(@Param("tourist_ids") List<Integer> touristIds);

    interface GradeAssignmentProjection {
        Integer getProtocolId();
        LocalDate getProtocolDate();
        Integer getTouristId();
        Grade getGrade ();
        KindOfTourism getKindOfTourism();
    }

    @SuppressWarnings("unused")
    interface ProtocolProjection {
        Integer getId();
        String getNumber();
        LocalDate getDate();
    }

    interface ReportAssignmentProjection {
        String getFirstName();
        String getLastName();
        String getMiddleName();
        String getClub();
        String getGradeTitle();
        int getExpiresInYears();
        String getKindOfTourismTitle();
        LocalDate getAssignmentDate();

    }
}
