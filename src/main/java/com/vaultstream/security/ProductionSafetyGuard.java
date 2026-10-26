package com.vaultstream.security;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Prevents an environment-variable override from exposing customer onboarding
 * or ledger mutations in the production API before the required launch gates
 * have been completed.
 */
@Component
@Profile("prod & !migration")
public class ProductionSafetyGuard {

    private final boolean registrationEnabled;
    private final boolean manualAdjustmentsEnabled;
    private final boolean customerTransfersEnabled;

    public ProductionSafetyGuard(
            @Value("${vaultstream.security.registration-enabled:false}") boolean registrationEnabled,
            @Value("${vaultstream.ledger.manual-adjustments-enabled:false}") boolean manualAdjustmentsEnabled,
            @Value("${vaultstream.ledger.customer-transfers-enabled:false}") boolean customerTransfersEnabled) {
        this.registrationEnabled = registrationEnabled;
        this.manualAdjustmentsEnabled = manualAdjustmentsEnabled;
        this.customerTransfersEnabled = customerTransfersEnabled;
    }

    @PostConstruct
    void enforceLaunchGates() {
        if (registrationEnabled || manualAdjustmentsEnabled || customerTransfersEnabled) {
            throw new IllegalStateException(
                    "Production registration and ledger mutations must remain disabled until legal, "
                            + "provider, and operational launch gates are approved");
        }
    }
}
