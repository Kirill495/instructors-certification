package org.tourism.instructors.application.protocol.outbox;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProtocolOutboxMetrics {

    private final JdbcClient jdbcClient;
    private final MeterRegistry meterRegistry;
    private volatile MetricsData metricsData = new MetricsData(0, 0, 0);

    @PostConstruct
    public void register() {
        Gauge.builder("protocols.outbox.rows", this, m -> m.metricsData.pendingRows())
                .tag("state", "pending")
                .register(meterRegistry);
        Gauge.builder("protocols.outbox.rows", this, m -> m.metricsData.deadRows())
                .tag("state", "dead")
                .register(meterRegistry);
        Gauge.builder("protocols.outbox.oldest.pending.age", this, m -> m.metricsData.maxAge())
                .baseUnit("seconds")
                .register(meterRegistry);
    }

    @Scheduled(fixedRate = 5L, timeUnit = TimeUnit.MINUTES)
    public void refreshMetrics() {
        metricsData =
                jdbcClient
                        .sql(
                                """
                                    SELECT
                                        count(*) FILTER ( WHERE sent_at IS NULL AND dead_at IS NULL) as pending_rows,
                                        count(*) FILTER ( WHERE dead_at IS NOT NULL) as dead_rows,
                                        COALESCE(EXTRACT(EPOCH FROM now() - min(created_at) FILTER ( WHERE sent_at IS NULL AND dead_at IS NULL)), 0) as max_age
                                    FROM
                                        instructors_grades.published_protocols_outbox
                                """)
                        .query(MetricsData.class)
                        .single();
    }

    private record MetricsData(int pendingRows, int deadRows, int maxAge) {}
}
