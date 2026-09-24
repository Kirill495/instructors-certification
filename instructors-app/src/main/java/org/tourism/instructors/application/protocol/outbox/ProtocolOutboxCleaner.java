package org.tourism.instructors.application.protocol.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@EnableConfigurationProperties(CleanUpProperties.class)
@RequiredArgsConstructor
public class ProtocolOutboxCleaner {

    private static final String DELETE_SQL =
            """
        DELETE FROM
            instructors_grades.published_protocols_outbox
        WHERE
            sent_at IS NOT NULL
            AND sent_at < now() - make_interval(days => :retention_days)
    """;

    private final CleanUpProperties properties;
    private final JdbcClient jdbcClient;

    @Scheduled(cron = "${protocols.outbox.cleanup-cron}")
    public void cleanOldProtocols() {
        int deleted =
                jdbcClient
                        .sql(DELETE_SQL)
                        .param("retention_days", properties.getRetentionDays())
                        .update();
        log.info(
                "Удалено {} отправленных записей outbox старше {} дней",
                deleted,
                properties.getRetentionDays());
    }
}
