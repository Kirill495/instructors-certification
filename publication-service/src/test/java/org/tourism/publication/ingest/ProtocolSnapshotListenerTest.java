package org.tourism.publication.ingest;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.tourism.publication.contract.AssignmentSnapshot;
import org.tourism.publication.contract.ProtocolSnapshot;
import org.tourism.publication.ingest.exception.IncorrectMessageKeyException;
import org.tourism.publication.ingest.exception.IncorrectProtocolIdException;
import org.tourism.publication.ingest.exception.UnsupportedSnapshotVersionException;

@ExtendWith(MockitoExtension.class)
class ProtocolSnapshotListenerTest {

    private static final String TOPIC_NAME = "protocols.snapshots";
    private static final String PROTOCOL_NUMBER = "p-number";
    private static final int PROTOCOL_ID = 1;
    private static final int CORRECT_VERSION = 1;
    private static final int INCORRECT_VERSION = 2;
    @Mock ProtocolIngestService service;

    @InjectMocks ProtocolSnapshotListener listener;

    @Test
    void testOnProtocolSnapshot_whenRecordContainsSnapshot_thenSnapshotShouldBeApplied() {
        int protocolId = 1;
        ProtocolSnapshot snapshot = buildProtocol(protocolId, CORRECT_VERSION);
        listener.onProtocolSnapshot(
                new ConsumerRecord<>(TOPIC_NAME, 0, 0L, String.valueOf(protocolId), snapshot));
        verify(service).apply(protocolId, snapshot);
        verifyNoMoreInteractions(service);
    }

    @Test
    void testOnProtocolSnapshot_whenRecordNotContainsSnapshot_thenSnapshotShouldBeRemoved() {
        listener.onProtocolSnapshot(
                new ConsumerRecord<>(TOPIC_NAME, 0, 0L, String.valueOf(PROTOCOL_ID), null));
        verify(service).remove(PROTOCOL_ID);
        verifyNoMoreInteractions(service);
    }

    @Test
    void testOnProtocolSnapshot_whenRecordContainsNonIntegerMessageKey_thenShouldThrow() {
        ProtocolSnapshot snapshot = buildProtocol(PROTOCOL_ID, CORRECT_VERSION);

        assertThrows(
                IncorrectMessageKeyException.class,
                () ->
                        listener.onProtocolSnapshot(
                                new ConsumerRecord<>(TOPIC_NAME, 0, 0L, "1-f", snapshot)));

        verifyNoInteractions(service);
    }

    @Test
    void
            testOnProtocolSnapshot_whenRecordContainsNonIntegerMessageKeyAndIncorrectVersion_thenShouldThrow() {
        ProtocolSnapshot snapshot = buildProtocol(PROTOCOL_ID, INCORRECT_VERSION);

        assertThrows(
                IncorrectMessageKeyException.class,
                () ->
                        listener.onProtocolSnapshot(
                                new ConsumerRecord<>(TOPIC_NAME, 0, 0L, "1-f", snapshot)));

        verifyNoInteractions(service);
    }

    @Test
    void testOnProtocolSnapshot_whenProtocolSnapshotHasUnsuitableVersion_thenShouldThrow() {
        ProtocolSnapshot snapshot = buildProtocol(PROTOCOL_ID, INCORRECT_VERSION);

        assertThrows(
                UnsupportedSnapshotVersionException.class,
                () ->
                        listener.onProtocolSnapshot(
                                new ConsumerRecord<>(
                                        TOPIC_NAME, 0, 0L, String.valueOf(PROTOCOL_ID), snapshot)));

        verifyNoInteractions(service);
    }

    @Test
    void testOnProtocolSnapshot_whenProtocolSnapshotIdNotCorrespondToMessageKey_thenShouldThrow() {
        ProtocolSnapshot snapshot = buildProtocol(2, CORRECT_VERSION);

        assertThrows(
                IncorrectProtocolIdException.class,
                () ->
                        listener.onProtocolSnapshot(
                                new ConsumerRecord<>(
                                        TOPIC_NAME, 0, 0L, String.valueOf(PROTOCOL_ID), snapshot)));

        verifyNoInteractions(service);
    }

    @Test
    void
            testOnProtocolSnapshot_whenProtocolSnapshotIdNotCorrespondToMessageKeyAndVersionIsIncorrect_thenShouldThrow() {
        ProtocolSnapshot snapshot = buildProtocol(2, INCORRECT_VERSION);

        assertThrows(
                UnsupportedSnapshotVersionException.class,
                () ->
                        listener.onProtocolSnapshot(
                                new ConsumerRecord<>(
                                        TOPIC_NAME, 0, 0L, String.valueOf(PROTOCOL_ID), snapshot)));

        verifyNoInteractions(service);
    }

    @Test
    void testOnProtocolSnapshot_whenMessageKeyIsNull_thenShouldThrow() {
        ProtocolSnapshot snapshot = buildProtocol(PROTOCOL_ID, CORRECT_VERSION);

        assertThrows(
                IncorrectMessageKeyException.class,
                () ->
                        listener.onProtocolSnapshot(
                                new ConsumerRecord<>(TOPIC_NAME, 0, 0L, null, snapshot)));

        verifyNoInteractions(service);
    }

    private static ProtocolSnapshot buildProtocol(int protocolId, int version) {

        return new ProtocolSnapshot(
                version,
                protocolId,
                ProtocolSnapshotListenerTest.PROTOCOL_NUMBER,
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
}
