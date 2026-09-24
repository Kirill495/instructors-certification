package org.tourism.instructors.application.protocol.outbox;

import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("protocols.outbox")
@RequiredArgsConstructor
@Getter
public class CleanUpProperties {
    @Positive private final int retentionDays;
}
