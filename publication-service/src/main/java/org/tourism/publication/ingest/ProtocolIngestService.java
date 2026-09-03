package org.tourism.publication.ingest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tourism.publication.contract.ProtocolSnapshot;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ProtocolIngestService {

    private final JdbcClient jdbcClient;
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public void apply(int protocolId, ProtocolSnapshot protocolSnapshot) {
        removeInner(protocolId);

        SqlParameterSource[] batch =
                protocolSnapshot.assignments().stream()
                        .map(
                                a ->
                                        new MapSqlParameterSource()
                                                .addValue("protocol_id", protocolId)
                                                .addValue("protocol_date", protocolSnapshot.date())
                                                .addValue(
                                                        "order_number",
                                                        protocolSnapshot.orderNumber())
                                                .addValue(
                                                        "protocol_number",
                                                        protocolSnapshot.number())
                                                .addValue("row_num", a.rowNum())
                                                .addValue("last_name", a.lastName())
                                                .addValue("first_name", a.firstName())
                                                .addValue("middle_name", a.middleName())
                                                .addValue("grade", a.grade())
                                                .addValue("kind_of_tourism", a.kindOfTourism())
                                                .addValue("club", a.club())
                                                .addValue("assignment_date", a.assignmentDate())
                                                .addValue("valid_until", a.validUntil()))
                        .toArray(SqlParameterSource[]::new);
        String sql =
                """
        INSERT INTO published_assignments (protocol_id, protocol_date, order_number, protocol_number, row_num, last_name, first_name, middle_name, grade, kind_of_tourism, club, assignment_date, valid_until)
        VALUES (:protocol_id, :protocol_date, :order_number, :protocol_number, :row_num, :last_name, :first_name, :middle_name, :grade, :kind_of_tourism, :club, :assignment_date, :valid_until)
        """;
        jdbcTemplate.batchUpdate(sql, batch);
        log.info(
                "Записан протокол {}. Строк: {}",
                protocolId,
                protocolSnapshot.assignments().size());
    }

    public void remove(int protocolId) {
        removeInner(protocolId);
        log.info("Удален протокол {}", protocolId);
    }

    private void removeInner(int protocolId) {
        jdbcClient
                .sql(
                        """
                DELETE FROM published_assignments WHERE protocol_id = :protocol_id
                    """)
                .param("protocol_id", protocolId)
                .update();
    }
}
