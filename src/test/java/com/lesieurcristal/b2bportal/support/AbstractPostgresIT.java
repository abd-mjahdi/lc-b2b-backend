package com.lesieurcristal.b2bportal.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Boots the full Spring context against a real PostgreSQL started by Testcontainers.
 * Liquibase runs on that database. Subclasses should use MockMvc, not mocked repositories.
 *
 * <p>The container is started once per JVM so multiple IT classes can share it.
 */
@SpringBootTest(properties = {
        "app.jwt.secret=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
        "spring.task.scheduling.enabled=false"
})
public abstract class AbstractPostgresIT {

    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("portail_b2b");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("DB_HOST", POSTGRES::getHost);
        registry.add("DB_PORT", () -> String.valueOf(POSTGRES.getMappedPort(5432)));
        registry.add("DB_NAME", POSTGRES::getDatabaseName);
        registry.add("DB_USERNAME", POSTGRES::getUsername);
        registry.add("DB_PASSWORD", POSTGRES::getPassword);

        registry.add("JWT_SECRET", () -> "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=");
        registry.add("JWT_EXPIRATION_MS", () -> "86400000");

        registry.add("MAIL_HOST", () -> "localhost");
        registry.add("MAIL_PORT", () -> "25");
        registry.add("MAIL_USERNAME", () -> "");
        registry.add("MAIL_PASSWORD", () -> "");
        registry.add("MAIL_SMTP_AUTH", () -> "false");
        registry.add("MAIL_SMTP_STARTTLS", () -> "false");

        registry.add("ACTIVATION_TOKEN_EXPIRATION_MS", () -> "604800000");
        registry.add("FRONTEND_BASE_URL", () -> "http://localhost:3000");
        registry.add("ACTIVATION_PATH", () -> "/activate");

        registry.add("spring.task.scheduling.enabled", () -> "false");
    }
}
