package com.vaultstream;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class VaultStreamApplication {

    public static void main(String[] args) {
        SpringApplication.run(VaultStreamApplication.class, args);
    }

}
