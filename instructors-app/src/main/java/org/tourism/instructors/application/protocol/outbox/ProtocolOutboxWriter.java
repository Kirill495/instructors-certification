package org.tourism.instructors.application.protocol.outbox;

import java.sql.Types;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.tourism.publication.contract.ProtocolSnapshot;
import tools.jackson.databind.json.JsonMapper;

@Component
public class ProtocolOutboxWriter {

    private static final String INSERT_SQL =
            """
            INSERT INTO instructors_grades.published_protocols_outbox(message_key, payload) values (:message_key, :payload)
            """;
    private final JdbcClient jdbcClient;
    private final JsonMapper jsonMapper;

    public ProtocolOutboxWriter(JdbcClient jdbcClient, JsonMapper jsonMapper) {
        this.jdbcClient = jdbcClient;
        this.jsonMapper = jsonMapper;
    }

    public void enqueueTombstone(String key) {
        insert(key, null);
    }

    public void enqueue(String key, ProtocolSnapshot snapshot) {
        String payload = jsonMapper.writeValueAsString(snapshot);
        insert(key, payload);
    }

    private void insert(String key, String payload) {
        jdbcClient
                .sql(INSERT_SQL)
                .param("message_key", key)
                .param("payload", payload, Types.VARCHAR)
                .update();
    }
}
