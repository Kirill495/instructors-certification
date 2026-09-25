package org.tourism.instructors.application.protocol.outbox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.tourism.instructors.application.protocol.ProtocolProducerService;
import org.tourism.instructors.application.protocol.exception.ProtocolPublishPermanentException;
import org.tourism.instructors.application.protocol.exception.ProtocolPublishRetryableException;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class ProtocolOutboxRelayIT {

    @ServiceConnection @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

    @Autowired JdbcClient jdbcClient;

    @MockitoBean ProtocolProducerService producer;
    ProtocolOutboxRelay relay;

    @BeforeEach
    void setUp() {
        relay = new ProtocolOutboxRelay(producer, jdbcClient);
        jdbcClient
                .sql(
                        """
        TRUNCATE TABLE instructors_grades.published_protocols_outbox;
        """)
                .update();
    }

    @Test
    void testSendRelay_WhenThrowsPermanentException_ThenAllMessagesProcessed() {
        insertRows();
        String cause = "cause-1";
        doThrow(new ProtocolPublishPermanentException(new RuntimeException(cause)))
                .when(producer)
                .send(eq("1"), anyString());
        relay.send();
        List<Row> rows = fetchRows();
        assertEquals(2, rows.size());
        Row failedRow = rows.getFirst();
        assertEquals("1", failedRow.messageKey);
        assertNull(failedRow.sentAt);
        assertNotNull(failedRow.deadAt);
        assertTrue(failedRow.errorMessage.contains(cause));
        assertEquals(1, failedRow.attempts);

        Row sentRow = rows.getLast();
        assertEquals("2", sentRow.messageKey);
        assertNotNull(sentRow.sentAt);
        assertNull(sentRow.deadAt);
        assertNull(sentRow.errorMessage);
        assertEquals(0, sentRow.attempts);
    }

    @Test
    void testSendRelay_WhenThrowsRetryableException_ThenRemainingRowsNotProcessed() {
        insertRows();
        String cause = "cause-1";
        doThrow(new ProtocolPublishRetryableException(new RuntimeException(cause)))
                .when(producer)
                .send(eq("1"), anyString());
        relay.send();
        List<Row> rows = fetchRows();
        assertEquals(2, rows.size());
        Row failedRow = rows.getFirst();
        assertEquals("1", failedRow.messageKey);
        assertNull(failedRow.sentAt);
        assertNull(failedRow.deadAt);
        assertNull(failedRow.errorMessage);
        assertEquals(1, failedRow.attempts);

        Row sentRow = rows.getLast();
        assertEquals("2", sentRow.messageKey);
        assertNull(sentRow.sentAt);
        assertNull(sentRow.deadAt);
        assertNull(sentRow.errorMessage);
        assertEquals(0, sentRow.attempts);
    }

    @Test
    void testSendRelay_WhenOutboxContainsOnlyDeadRow_ThenNothingProcessed() {

        insertDeadRow();
        relay.send();
        verify(producer, times(0)).send(anyString(), anyString());
    }

    private void insertRows() {
        jdbcClient
                .sql(
                        """
                    INSERT INTO instructors_grades.published_protocols_outbox (message_key, payload)
                    VALUES ('1', 'payload-1'), ('2', 'payload-2');
                """)
                .update();
    }

    private void insertDeadRow() {

        jdbcClient
                .sql(
                        """
                        INSERT INTO instructors_grades.published_protocols_outbox (message_key, payload, dead_at)
                        VALUES ('1', 'payload-1', now());
                    """)
                .update();
    }

    private List<Row> fetchRows() {
        List<Row> rows =
                jdbcClient
                        .sql(
                                """
                            SELECT
                                message_key, sent_at, dead_at, error_message, attempts
                            FROM
                                instructors_grades.published_protocols_outbox;
                        """)
                        .query(Row.class)
                        .stream()
                        .sorted(Comparator.comparing(row -> row.messageKey))
                        .toList();
        return rows;
    }

    record Row(
            String messageKey, Instant sentAt, Instant deadAt, String errorMessage, int attempts) {}
}
