package org.tourism.publication.registry;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.tourism.publication.registry.dto.AssignmentResponse;
import org.tourism.publication.registry.dto.ProtocolResponse;

@Service
@RequiredArgsConstructor
public class ProtocolRegistry {

    private final JdbcClient jdbcClient;

    public Optional<ProtocolResponse> findProtocolByNumber(String number) {
        List<AssignmentFlatRow> assignments =
                jdbcClient
                        .sql(
                                """
                        SELECT
                           a.protocol_date AS protocol_date
                           , a.protocol_id AS protocol_id
                           , a.order_number AS order_number
                           , a.protocol_number AS protocol_number
                           , a.row_num AS row_num
                           , a.last_name AS last_name
                           , a.first_name AS first_name
                           , a.middle_name AS middle_name
                           , a.grade AS grade
                           , a.kind_of_tourism AS kind_of_tourism
                           , a.club AS club
                           , a.assignment_date AS assignment_date
                           , a.valid_until as valid_until
                        FROM
                           published_assignments AS a
                        WHERE
                           a.protocol_number = :protocol_number
                        ORDER BY
                           a.protocol_id, a.row_num
                           """)
                        .param("protocol_number", number)
                        .query(AssignmentFlatRow.class)
                        .list();
        Optional<ProtocolResponse> responseOpt =
                assignments.stream()
                        .collect(
                                Collectors.groupingBy(
                                        row ->
                                                new ProtocolHeader(
                                                        row.protocolId(),
                                                        row.protocolNumber(),
                                                        row.protocolDate(),
                                                        row.orderNumber()),
                                        LinkedHashMap::new,
                                        Collectors.mapping(
                                                row ->
                                                        new AssignmentResponse(
                                                                row.rowNum(),
                                                                row.lastName(),
                                                                row.firstName(),
                                                                row.middleName(),
                                                                row.grade(),
                                                                row.kindOfTourism(),
                                                                row.club(),
                                                                row.assignmentDate(),
                                                                row.validUntil()),
                                                Collectors.toList())))
                        .entrySet()
                        .stream()
                        .map(
                                entry ->
                                        new ProtocolResponse(
                                                entry.getKey().number(),
                                                entry.getKey().protocolDate(),
                                                entry.getKey().orderNumber(),
                                                entry.getValue()))
                        .findFirst();
        return responseOpt;
    }

    private record ProtocolHeader(
            int protocolId, String number, LocalDate protocolDate, String orderNumber) {}

    private record AssignmentFlatRow(
            int protocolId,
            String protocolNumber,
            LocalDate protocolDate,
            String orderNumber,
            int rowNum,
            String lastName,
            String firstName,
            String middleName,
            String grade,
            String kindOfTourism,
            String club,
            LocalDate assignmentDate,
            LocalDate validUntil) {}
}
