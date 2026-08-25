package org.tourism.publication.registry;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.tourism.publication.registry.dto.ProtocolResponse;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@JdbcTest
@Sql("/test-data.sql")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(ProtocolRegistry.class)
@Testcontainers
@TestPropertySource(properties = {
        "logging.level.org.springframework.jdbc.core.JdbcTemplate=DEBUG",
        "logging.level.org.springframework.jdbc.core.StatementCreatorUtils=TRACE"
})

class ProtocolRegistryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");
    @Autowired
    ProtocolRegistry protocolRegistry;

    @Test
    void testGetProtocolByNumber() {
        Optional<ProtocolResponse> responseOpt = protocolRegistry.findProtocolByNumber("15");
        assertTrue(responseOpt.isPresent());
        ProtocolResponse response = responseOpt.get();
        assertEquals("15", response.number());
        assertEquals(LocalDate.of(2026, 3, 14), response.date());
        assertEquals("7", response.orderNumber());
        assertEquals(3, response.assignments().size());
        assertEquals("Петрова", response.assignments().get(0).lastName());
        assertEquals("Иванов", response.assignments().get(1).lastName());
        assertEquals("Сидоров", response.assignments().get(2).lastName());
    }
}