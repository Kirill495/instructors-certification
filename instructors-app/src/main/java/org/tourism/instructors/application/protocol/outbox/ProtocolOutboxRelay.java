package org.tourism.instructors.application.protocol.outbox;

import java.sql.Types;
import java.time.Clock;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.tourism.instructors.application.protocol.ProtocolProducerService;
import org.tourism.instructors.application.protocol.exception.ProtocolPublishException;

@Slf4j
@ConditionalOnProperty(
        name = "protocols.outbox.relay.enabled",
        havingValue = "true",
        matchIfMissing = true)
@Component
@RequiredArgsConstructor
public class ProtocolOutboxRelay {

    private static final int ATTEMPTS_THRESHOLD = 10;

    private final ProtocolProducerService protocolProducerService;
    private final JdbcClient jdbcClient;
    private final Clock clock;

    @Scheduled(fixedDelayString = "1000")
    public void send() {

        List<Row> rows = readRows();
        for (Row row : rows) {
            try {
                protocolProducerService.send(row.messageKey(), row.payload());
                markSent(row);
            } catch (ProtocolPublishException e) {
                markFailed(row, e);
                break;
            }
        }
    }

    private List<Row> readRows() {
        List<Row> rows =
                jdbcClient
                        .sql(
"""
SELECT id, message_key, payload, attempts FROM instructors_grades.published_protocols_outbox WHERE sent_at is null order by id
""")
                        .withMaxRows(100)
                        .query(Row.class)
                        .list();
        return rows;
    }

    private void markFailed(Row row, Exception e) {
        int currentAttempt = row.attempts() + 1;
        jdbcClient
                .sql(
"""
UPDATE instructors_grades.published_protocols_outbox SET attempts = :attempts WHERE id = :id
""")
                .param("id", row.id, Types.BIGINT)
                .param("attempts", currentAttempt, Types.INTEGER)
                .update();
        if (currentAttempt < ATTEMPTS_THRESHOLD) {
            log.warn(
                    "Ошибка при отправке протокола id={}, messageKey={}, попытка={}",
                    row.id(),
                    row.messageKey(),
                    currentAttempt,
                    e);
        } else if (currentAttempt >= ATTEMPTS_THRESHOLD) {
            log.error(
                    "Ошибка при отправке протокола id={}, messageKey={}, попытка={}. "
                            + "Достигнут максимальный порог повторных попыток",
                    row.id(),
                    row.messageKey(),
                    currentAttempt,
                    e);
        }
    }

    private void markSent(Row row) {
        jdbcClient
                .sql(
"""
UPDATE instructors_grades.published_protocols_outbox SET sent_at = now() WHERE id = :id
""")
                .param("id", row.id)
                .update();
    }

    private record Row(long id, String messageKey, String payload, int attempts) {}
}
