package org.tourism.instructors.application.protocol.exception;

public abstract sealed class ProtocolPublishException extends RuntimeException
        permits ProtocolPublishPermanentException, ProtocolPublishRetryableException {

    public ProtocolPublishException(Throwable cause) {
        super(cause);
    }
}
