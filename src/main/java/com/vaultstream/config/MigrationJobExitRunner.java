package com.vaultstream.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("migration")
@RequiredArgsConstructor
public class MigrationJobExitRunner implements ApplicationRunner {

    private final ConfigurableApplicationContext applicationContext;

    @Override
    public void run(ApplicationArguments args) {
        applicationContext.close();
    }
}
