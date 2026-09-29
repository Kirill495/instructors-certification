package org.tourism.instructors.application.protocol.outbox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Значения читаются через {@link MeterRegistry}, а не из {@code MetricsData}: половина ошибок этого
 * класса жила не в запросе, а в связке поля с gauge, и видна только со стороны реестра.
 *
 * <p>Бин и реестр берутся из контекста, поэтому тест заодно проверяет {@code @Component} и
 * {@code @PostConstruct} — без них измерители просто не были бы зарегистрированы.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class ProtocolOutboxMetricsIT {

    @ServiceConnection @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

    @Autowired JdbcClient jdbcClient;

    @Autowired MeterRegistry meterRegistry;

    @Autowired ProtocolOutboxMetrics protocolOutboxMetrics;

    @BeforeEach
    void setUp() {
        jdbcClient.sql("TRUNCATE TABLE instructors_grades.published_protocols_outbox").update();
    }

    @Test
    void testRefreshMetrics_WhenOutboxIsEmpty_ThenAllGaugesAreZero() {
        protocolOutboxMetrics.refreshMetrics();

        Gauges gauges = readGauges();

        assertEquals(0, gauges.pendingRows());
        assertEquals(0, gauges.deadRows());
        assertEquals(0, gauges.oldestPendingAgeSeconds());
    }

    /**
     * Gauge обязан читать поле в момент опроса, а не запоминать значение при регистрации.
     * Измеритель, привязанный к исходному экземпляру {@code MetricsData}, продолжает отдавать нули
     * — отличить его от исправного можно только вторым обновлением на изменившихся данных.
     */
    @Test
    void testRefreshMetrics_WhenDataChangesBetweenRefreshes_ThenGaugesFollow() {
        protocolOutboxMetrics.refreshMetrics();
        assertEquals(0, readGauges().pendingRows());

        insertRows(
                """
                (now() - interval '15 seconds', null, null,  '1'),
                (now() - interval '10 seconds', null, now(), '2'),
                (now() - interval '5 seconds',  null, now(), '3')
                """);
        protocolOutboxMetrics.refreshMetrics();

        Gauges gauges = readGauges();

        assertEquals(1, gauges.pendingRows());
        assertEquals(2, gauges.deadRows());
        assertAgeBetween(gauges, 15, 20);
    }

    /**
     * Все три агрегата должны отбирать строки одним предикатом. Мёртвая запись часовой давности и
     * отправленная девяностосекундной старше любой живой неотправленной, поэтому потеря {@code
     * dead_at IS NULL} или {@code sent_at IS NULL} в фильтре возраста даёт 3600 или 90 вместо 10.
     * Количества по состояниям различаются намеренно: при одинаковых перестановка тегов {@code
     * state} осталась бы незамеченной.
     */
    @Test
    void testRefreshMetrics_WhenRowsInAllThreeStates_ThenOnlyLiveUnsentRowsCount() {
        insertRows(
                """
                (now() - interval '10 seconds', null,  null,  '1'),
                (now() - interval '5 seconds',  null,  null,  '2'),
                (now() - interval '1 minute',   now(), null,  '3'),
                (now() - interval '90 seconds', now(), null,  '4'),
                (now() - interval '1 hour',     null,  now(), '5')
                """);

        protocolOutboxMetrics.refreshMetrics();

        Gauges gauges = readGauges();

        assertEquals(2, gauges.pendingRows());
        assertEquals(1, gauges.deadRows());
        assertAgeBetween(gauges, 10, 15);
    }

    /** Мёртвые записи не удаляются никогда, поэтому возраст обязан их игнорировать, а не расти. */
    @Test
    void testRefreshMetrics_WhenOnlyDeadRowsLeft_ThenPendingAndAgeAreZero() {
        insertRows("(now() - interval '1 hour', null, now(), '1')");

        protocolOutboxMetrics.refreshMetrics();

        Gauges gauges = readGauges();

        assertEquals(0, gauges.pendingRows());
        assertEquals(1, gauges.deadRows());
        assertEquals(0, gauges.oldestPendingAgeSeconds());
    }

    /**
     * Время считается в SQL, а не в JVM: иначе тест сравнивал бы метку от одних часов с {@code
     * now()} от других.
     */
    private void insertRows(String valuesClause) {
        jdbcClient
                .sql(
                        """
                        INSERT INTO instructors_grades.published_protocols_outbox
                            (created_at, sent_at, dead_at, message_key)
                        VALUES
                        """
                                + valuesClause)
                .update();
    }

    private void assertAgeBetween(Gauges gauges, int minSeconds, int maxSeconds) {
        int age = gauges.oldestPendingAgeSeconds();
        assertTrue(
                age >= minSeconds && age < maxSeconds,
                "ожидался возраст в [%d, %d) секунд, получено %d"
                        .formatted(minSeconds, maxSeconds, age));
    }

    private Gauges readGauges() {
        return new Gauges(
                readGauge("protocols.outbox.rows", "state", "pending"),
                readGauge("protocols.outbox.rows", "state", "dead"),
                readGauge("protocols.outbox.oldest.pending.age"));
    }

    /** Поиск через {@code get} падает с {@code MeterNotFoundException}, если имя или тег не те. */
    private int readGauge(String name, String tagKey, String tagValue) {
        return toSeconds(name, meterRegistry.get(name).tag(tagKey, tagValue).gauge().value());
    }

    private int readGauge(String name) {
        return toSeconds(name, meterRegistry.get(name).gauge().value());
    }

    /**
     * {@code (int) Double.NaN} равно нулю, поэтому собранный сборщиком мусора источник gauge
     * выглядел бы как честный ноль. Проверяем до приведения.
     */
    private static int toSeconds(String name, double value) {
        assertFalse(
                Double.isNaN(value),
                "%s отдал NaN — вероятно, источник gauge собран GC".formatted(name));
        return (int) value;
    }

    private record Gauges(int pendingRows, int deadRows, int oldestPendingAgeSeconds) {}
}
