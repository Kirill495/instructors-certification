package org.tourism.publication.ingest.exception;

public class UnsupportedSnapshotVersionException extends RuntimeException {
  public UnsupportedSnapshotVersionException(String message) {
    super(message);
  }
}
