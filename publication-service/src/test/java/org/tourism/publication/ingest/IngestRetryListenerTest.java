package org.tourism.publication.ingest;

import static org.junit.jupiter.api.Assertions.*;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class IngestRetryListenerTest {

    MeterRegistry meterRegistry;
    IngestRetryListener ingestRetryListener;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        ingestRetryListener = new IngestRetryListener(meterRegistry);
    }

    @Test
    void testRecovered_WhenNewRecordRecovered_ThenCounterIncremented() {
        ingestRetryListener.recovered(getRecord(), new RuntimeException());

        assertEquals(1, meterRegistry.getMeters().size());
        Counter counter =
                meterRegistry
                        .get("publication.ingest.dlt.records")
                        .tag("result", "sent")
                        .tag("exception", "RuntimeException")
                        .counter();
        assertEquals(1, counter.count());
    }

    @Test
    void testRecovered_WhenExceptionIsNull_ThenExceptionTagIsNone() {
        ingestRetryListener.recovered(getRecord(), null);

        assertEquals(1, meterRegistry.getMeters().size());
        Counter counter =
                meterRegistry
                        .get("publication.ingest.dlt.records")
                        .tag("result", "sent")
                        .tag("exception", "none")
                        .counter();
        assertEquals(1, counter.count());
    }

    @Test
    void testRecoveryFailed_WhenRecordFailedToRecover_ThenCounterIncremented() {
        ingestRetryListener.recoveryFailed(
                getRecord(), new IllegalArgumentException(), new NullPointerException());

        assertEquals(1, meterRegistry.getMeters().size());
        Counter counter =
                meterRegistry
                        .get("publication.ingest.dlt.records")
                        .tag("result", "failed")
                        .tag("exception", "NullPointerException")
                        .counter();
        assertEquals(1, counter.count());
    }

    private static ConsumerRecord<String, String> getRecord() {
        return new ConsumerRecord<>("t", 1, 1L, "1", "1");
    }
}
