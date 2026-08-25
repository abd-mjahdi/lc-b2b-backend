package com.lesieurcristal.b2bportal.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

/**
 * Boots the full Spring context against real PostgreSQL and MinIO started by Testcontainers.
 * Liquibase runs on that database. Subclasses should use MockMvc, not mocked repositories.
 *
 * <p>The containers are started once per JVM so multiple IT classes can share them.
 */
@SpringBootTest(properties = {
        "app.jwt.secret=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
        "spring.task.scheduling.enabled=false"
})
public abstract class AbstractPostgresIT {

    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("portail_b2b");

    static final GenericContainer<?> MINIO = new GenericContainer<>(
            DockerImageName.parse("minio/minio:RELEASE.2025-04-22T22-12-26Z"))
            .withEnv("MINIO_ROOT_USER", "minioadmin")
            .withEnv("MINIO_ROOT_PASSWORD", "minioadmin")
            .withCommand("server", "/data")
            .withExposedPorts(9000)
            .waitingFor(Wait.forHttp("/minio/health/live").forPort(9000).forStatusCode(200)
                    .withStartupTimeout(Duration.ofSeconds(60)));

    static {
        POSTGRES.start();
        MINIO.start();
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

        registry.add("S3_ENDPOINT", () -> "http://" + MINIO.getHost() + ":" + MINIO.getMappedPort(9000));
        registry.add("S3_REGION", () -> "us-east-1");
        registry.add("S3_BUCKET", () -> "lc-b2b-documents");
        registry.add("S3_ACCESS_KEY", () -> "minioadmin");
        registry.add("S3_SECRET_KEY", () -> "minioadmin");
        registry.add("S3_PATH_STYLE_ACCESS", () -> "true");
    }
}
