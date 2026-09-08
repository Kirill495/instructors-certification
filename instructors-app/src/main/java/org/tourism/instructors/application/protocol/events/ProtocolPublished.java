package org.tourism.instructors.application.protocol.events;

public class ProtocolPublished extends RuntimeException {
  public ProtocolPublished(String message) {
    super(message);
  }
}
