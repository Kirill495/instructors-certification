package org.tourism.publication.ingest;

import io.micrometer.core.instrument.MeterRegistry;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.jspecify.annotations.Nullable;
import org.springframework.kafka.listener.RetryListener;

@Slf4j
@RequiredArgsConstructor
public class IngestRetryListener implements RetryListener {

    private static final int THRESHOLD_ATTEMPT_COUNT = 50;

    private final MeterRegistry meterRegistry;

    @Override
    public void failedDelivery(
            ConsumerRecord<?, ?> record, @Nullable Exception ex, int deliveryAttempt) {
        if ((deliveryAttempt > 0) && (deliveryAttempt % THRESHOLD_ATTEMPT_COUNT) == 0) {
            log.warn(
                    "Повторная обработка записи messageKey={}. Попытка #{}.",
                    record.key(),
                    deliveryAttempt,
                    ex);
        }
    }

    @Override
    public void recoveryFailed(
            ConsumerRecord<?, ?> record, @Nullable Exception original, Exception failure) {
        meterRegistry
                .counter(
                        "publication.ingest.dlt.records",
                        "exception",
                        failure.getClass().getSimpleName(),
                        "result",
                        "failed")
                .increment();
        log.error(
                "Ошибка при восстановлении записи messageKey={}. Исходная ошибка={}",
                record.key(),
                original,
                failure);
    }

    @Override
    public void recovered(ConsumerRecord<?, ?> record, @Nullable Exception ex) {
        meterRegistry
                .counter(
                        "publication.ingest.dlt.records",
                        "exception",
                        Objects.isNull(ex) ? "none" : ex.getClass().getSimpleName(),
                        "result",
                        "sent")
                .increment();
        log.warn("Сообщение messageKey={} перенесено в DLT", record.key(), ex);
    }
}
