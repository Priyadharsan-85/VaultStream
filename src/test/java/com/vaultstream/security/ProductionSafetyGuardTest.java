package com.vaultstream.security;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

class ProductionSafetyGuardTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(GuardConfiguration.class)
            .withPropertyValues("spring.profiles.active=prod");

    @Test
    void startsWhenProductionMoneyMovementAndRegistrationAreDisabled() {
        contextRunner.run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    void refusesToStartWhenAnyProductionLaunchGateIsOverridden() {
        contextRunner
                .withPropertyValues("vaultstream.ledger.customer-transfers-enabled=true")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseInstanceOf(IllegalStateException.class)
                            .hasStackTraceContaining("must remain disabled");
                });
    }

    @Test
    void doesNotRunInTheMigrationProfile() {
        contextRunner
                .withPropertyValues(
                        "spring.profiles.active=prod,migration",
                        "vaultstream.ledger.customer-transfers-enabled=true")
                .run(context -> assertThat(context).hasNotFailed());
    }

    @Configuration(proxyBeanMethods = false)
    @Import(ProductionSafetyGuard.class)
    static class GuardConfiguration {
    }
}
