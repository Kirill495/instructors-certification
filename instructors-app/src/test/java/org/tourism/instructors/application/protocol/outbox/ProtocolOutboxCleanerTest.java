package org.tourism.instructors.application.protocol.outbox;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.validation.BindValidationException;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class ProtocolOutboxCleanerTest {

    @EnableConfigurationProperties(CleanUpProperties.class)
    static class TestConfig {}

    @Test
    void testContextCreation_WhenRetentionDaysIsZero_ThenContextShouldNotCreated() {
        new ApplicationContextRunner()
                .withUserConfiguration(TestConfig.class)
                .withPropertyValues("protocols.outbox.retention-days=0")
                .run(
                        context ->
                                assertThat(context)
                                        .hasFailed()
                                        .getFailure()
                                        .rootCause()
                                        .isInstanceOf(BindValidationException.class)
                                        .hasMessageContaining("retentionDays"));
    }

    @Test
    void testContextCreation_WhenRetentionDaysIsPositive_ThenContextShouldCreate() {
        new ApplicationContextRunner()
                .withUserConfiguration(TestConfig.class)
                .withPropertyValues("protocols.outbox.retention-days=1")
                .run(
                        context ->
                                assertThat(
                                                context.getBean(CleanUpProperties.class)
                                                        .getRetentionDays())
                                        .isEqualTo(1));
    }
}
