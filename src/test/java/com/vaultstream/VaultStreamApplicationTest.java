package com.vaultstream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
class VaultStreamApplicationTest {

    @Test
    void contextLoads() {
        // Just verifying that the Spring application context loads successfully
        assertTrue(true);
    }
}