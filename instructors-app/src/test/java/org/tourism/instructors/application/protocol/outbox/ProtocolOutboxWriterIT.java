package org.tourism.instructors.application.protocol.outbox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.tourism.publication.contract.AssignmentSnapshot;
import org.tourism.publication.contract.ProtocolSnapshot;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class ProtocolOutboxWriterIT {

    @ServiceConnection @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

    @Autowired ProtocolOutboxWriter protocolOutboxWriter;
    @Autowired TransactionTemplate transactionTemplate;
    @Autowired JdbcClient jdbcClient;

    @BeforeEach
    void setUp() {
        jdbcClient.sql("TRUNCATE instructors_grades.published_protocols_outbox").update();
    }

    @Test
    void testEnqueue_whenTransactionRollBacks_thenPayloadNotInsertedInOutbox() {
        LocalDate date = LocalDate.of(2026, 9, 1);
        Instant instant = Instant.parse("2026-09-01T10:00:00Z");
        LocalDate validUntil = date.plusYears(1);
        AssignmentSnapshot aSnapshot =
                new AssignmentSnapshot(
                        1,
                        "Федоров",
                        "Валерий",
                        "А",
                        "инструктор",
                        "горный",
                        "МИЭМ",
                        date,
                        validUntil);
        int protocolId = 101;
        String protocolIdStr = String.valueOf(protocolId);
        ProtocolSnapshot snapshot =
                new ProtocolSnapshot(
                        1, protocolId, "num", date, "order-num", instant, List.of(aSnapshot));

        transactionTemplate.execute(
                a -> {
                    protocolOutboxWriter.enqueue(protocolIdStr, snapshot);
                    a.setRollbackOnly();
                    return null;
                });
        assertEquals(0, countOutboxRows());
    }

    @Test
    void testEnqueue_whenTransactionCommits_thenOutboxContainsProtocolPayload() {
        LocalDate date = LocalDate.of(2026, 9, 1);
        Instant instant = Instant.parse("2026-09-01T10:00:00Z");
        LocalDate validUntil = date.plusYears(1);

        String payload =
                """
                {"version":1,"protocolId":101,"number":"num","date":"2026-09-01",\
                "orderNumber":"order-num","publishedAt":"2026-09-01T10:00:00Z",\
                "assignments":[{"rowNum":1,"lastName":"Федоров","firstName":"Валерий","middleName":"А","grade":\
                "инструктор","kindOfTourism":"горный","club":"МИЭМ","assignmentDate":"2026-09-01","validUntil":"2027-09-01"}]}\
                """;

        AssignmentSnapshot aSnapshot =
                new AssignmentSnapshot(
                        1,
                        "Федоров",
                        "Валерий",
                        "А",
                        "инструктор",
                        "горный",
                        "МИЭМ",
                        date,
                        validUntil);
        int protocolId = 101;
        String protocolIdStr = String.valueOf(protocolId);
        ProtocolSnapshot snapshot =
                new ProtocolSnapshot(
                        1, protocolId, "num", date, "order-num", instant, List.of(aSnapshot));

        transactionTemplate.execute(
                status -> {
                    protocolOutboxWriter.enqueue(protocolIdStr, snapshot);
                    return null;
                });
        List<Row> rows = selectRows(protocolIdStr);

        assertEquals(1, rows.size());
        Row row = rows.getFirst();
        assertEquals(payload, row.payload);
        assertEquals(protocolIdStr, row.messageKey);
        assertEquals(0, row.attempts);
        assertNull(row.sentAt);
    }

    @Test
    void testEnqueueTombstone_whenTransactionCommits_thenOutboxContainsNull() {
        int protocolId = 101;
        String protocolIdStr = String.valueOf(protocolId);

        transactionTemplate.execute(
                status -> {
                    protocolOutboxWriter.enqueueTombstone(protocolIdStr);
                    return null;
                });
        List<Row> rows = selectRows(protocolIdStr);

        assertEquals(1, rows.size());
        Row row = rows.getFirst();
        assertNull(row.payload);
        assertEquals(protocolIdStr, row.messageKey);
        assertEquals(0, row.attempts);
        assertNull(row.sentAt);
    }

    private List<Row> selectRows(String protocolIdStr) {
        List<Row> rows =
                jdbcClient
                        .sql(
                                "SELECT message_key, payload, sent_at, attempts FROM instructors_grades"
                                        + ".published_protocols_outbox WHERE message_key = :message_key")
                        .param("message_key", protocolIdStr)
                        .query(Row.class)
                        .list();
        return rows;
    }

    private record Row(String messageKey, String payload, Instant sentAt, int attempts) {}

    private int countOutboxRows() {

        return jdbcClient
                .sql("SELECT COUNT(*) FROM instructors_grades.published_protocols_outbox")
                .query(Integer.class)
                .single();
    }
}
