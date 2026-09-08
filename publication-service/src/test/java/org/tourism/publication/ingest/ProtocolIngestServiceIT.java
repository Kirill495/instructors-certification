package org.tourism.publication.ingest;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.jdbc.Sql;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.tourism.publication.contract.AssignmentSnapshot;
import org.tourism.publication.contract.ProtocolSnapshot;

@JdbcTest
@Sql("/test-data.sql")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(ProtocolIngestService.class)
@Testcontainers
class ProtocolIngestServiceTest {

    @Container @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @Autowired JdbcClient jdbcClient;
    @Autowired ProtocolIngestService service;

    @Test
    void testApply_WhenNewProtocolAdded_thenNewNewProtocolPersistInDB() {
        int protocolId = 10001;
        String protocolNumber = "n-1";
        ProtocolSnapshot snapshot = buildProtocol(protocolId, protocolNumber);
        service.apply(protocolId, snapshot);

        List<String> numbers = fetchProtocolNumberByProtocolId(protocolId);

        assertEquals(List.of(protocolNumber), numbers);
        verifyRowsCount(101, 3);
        verifyRowsCount(102, 1);
    }

    @Test
    void testApply_WhenExistingProtocolUpdated_thenExistingRewritten() {
        int protocolId = 101;
        String protocolNumber = "n-1";
        ProtocolSnapshot snapshot = buildProtocol(protocolId, protocolNumber);
        service.apply(protocolId, snapshot);

        List<String> numbers = fetchProtocolNumberByProtocolId(protocolId);
        assertEquals(1, numbers.size());
        assertEquals(protocolNumber, numbers.getFirst());
        verifyRowsCount(102, 1);
    }

    @Test
    void testApply_WhenExistingProtocolUpdatedTwice_thenNewPersisted() {
        int protocolId = 101;
        String protocolNumber = "n-1";
        ProtocolSnapshot snapshot = buildProtocol(protocolId, protocolNumber);
        service.apply(protocolId, snapshot);
        service.apply(protocolId, snapshot);
        List<String> numbers = fetchProtocolNumberByProtocolId(protocolId);
        assertEquals(1, numbers.size());
        assertEquals(protocolNumber, numbers.getFirst());
        verifyRowsCount(102, 1);
    }

    @Test
    void testRemove_whenProtocolExistsInTable_thenRemovedOnlyExistingProtocol() {
        int protocolId = 101;
        service.remove(protocolId);
        verifyRowsCount(protocolId, 0);
        verifyRowsCount(102, 1);
    }

    private List<String> fetchProtocolNumberByProtocolId(int protocolId) {
        return jdbcClient
                .sql(
                        """
                                SELECT protocol_number FROM publication.published_assignments WHERE protocol_id = :protocol_id""")
                .param("protocol_id", protocolId)
                .query(String.class)
                .list();
    }

    private static ProtocolSnapshot buildProtocol(int protocolId, String protocolNumber) {
        return new ProtocolSnapshot(
                1,
                protocolId,
                protocolNumber,
                LocalDate.of(2026, 1, 1),
                "order-1",
                Instant.parse("2026-09-01T00:00:00Z"),
                List.of(
                        new AssignmentSnapshot(
                                1,
                                "lastName",
                                "firstName",
                                "middleName",
                                "grade",
                                "kindOfTourism",
                                "club",
                                LocalDate.of(2026, 1, 1),
                                null)));
    }

    private void verifyRowsCount(int protocolId, int count) {
        List<String> result =
                jdbcClient
                        .sql(
                                """
                                SELECT protocol_number FROM publication.published_assignments WHERE protocol_id = :protocol_id""")
                        .param("protocol_id", protocolId)
                        .query(String.class)
                        .list();
        assertEquals(count, result.size());
    }
}
