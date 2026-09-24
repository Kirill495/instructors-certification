package org.tourism.instructors.application.protocol.outbox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = "protocols.outbox.retention-days=1")
@Testcontainers
class ProtocolOutboxCleanerIT {

    @ServiceConnection @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

    @Autowired CleanUpProperties cleanUpProperties;

    @Autowired JdbcClient jdbcClient;

    @Autowired ProtocolOutboxCleaner protocolOutboxCleaner;

    @BeforeEach
    void setUp() {
        jdbcClient.sql("TRUNCATE instructors_grades.published_protocols_outbox").update();
    }

    @Test
    void
            testCleanOldProtocols_WhenSentAtOlderThanRetentionJustOnOneMinute_ThenOldRowsDeletedAndRecentlySentKept() {

        jdbcClient
                .sql(
"""
        INSERT INTO instructors_grades.published_protocols_outbox(message_key, sent_at) VALUES
            ('1', now() - interval '1 day' - interval '1 minute'),
            ('2', now() - interval '1 day' + interval '1 minute')
""")
                .update();
        protocolOutboxCleaner.cleanOldProtocols();
        List<String> messageKeys =
                jdbcClient
                        .sql(
"""
SELECT message_key FROM instructors_grades.published_protocols_outbox
""")
                        .query(String.class)
                        .list();

        assertEquals(1, messageKeys.size());
        assertEquals("2", messageKeys.getFirst());
    }

    @Test
    void
            testCleanOldProtocols_WhenSentAtOlderThanRetention_ThenOldSentRowDeletedAndNotSentAndRecentlySentKept() {
        jdbcClient
                .sql(
                        """
        INSERT INTO instructors_grades.published_protocols_outbox(message_key, sent_at) VALUES ('1', '2026-01-01'), ('2', null), ('3', now())
        """)
                .update();

        protocolOutboxCleaner.cleanOldProtocols();
        List<String> messageKeys =
                jdbcClient
                        .sql(
                                """
                SELECT message_key FROM instructors_grades.published_protocols_outbox
                """)
                        .query(String.class)
                        .list();
        assertEquals(2, messageKeys.size());
        assertTrue(messageKeys.contains("2"));
        assertTrue(messageKeys.contains("3"));
    }
}
