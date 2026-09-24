package com.ticketing.support;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Uses Testcontainers Postgres when Docker is available. Otherwise uses a dedicated
 * local {@code ticketing_test} database so integration tests are not blocked on
 * docker.sock access and never touch H2.
 */
public abstract class PostgresIntegrationTest {

    private static final PostgreSQLContainer<?> POSTGRES;
    private static final Map<String, String> LOCAL_ENV = loadDotEnv();

    static {
        PostgreSQLContainer<?> container = null;
        if (DockerClientFactory.instance().isDockerAvailable()) {
            container = new PostgreSQLContainer<>("postgres:16-alpine");
            container.start();
        }
        POSTGRES = container;
    }

    @DynamicPropertySource
    static void registerDataSource(DynamicPropertyRegistry registry) {
        if (POSTGRES != null) {
            registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
            registry.add("spring.datasource.username", POSTGRES::getUsername);
            registry.add("spring.datasource.password", POSTGRES::getPassword);
            return;
        }
        registry.add(
                "spring.datasource.url",
                () -> "jdbc:postgresql://127.0.0.1:5432/ticketing_test");
        registry.add(
                "spring.datasource.username",
                () -> firstNonBlank(
                        System.getenv("DATABASE_USERNAME"),
                        LOCAL_ENV.get("DATABASE_USERNAME"),
                        "postgres"));
        registry.add(
                "spring.datasource.password",
                () -> firstNonBlank(
                        System.getenv("DATABASE_PASSWORD"),
                        LOCAL_ENV.get("DATABASE_PASSWORD"),
                        "postgres"));
    }

    private static Map<String, String> loadDotEnv() {
        Map<String, String> values = new LinkedHashMap<>();
        Path envFile = Path.of(".env");
        if (!Files.isRegularFile(envFile)) {
            return values;
        }
        try {
            for (String line : Files.readAllLines(envFile)) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#") || !trimmed.contains("=")) {
                    continue;
                }
                int separator = trimmed.indexOf('=');
                values.put(trimmed.substring(0, separator), trimmed.substring(separator + 1));
            }
        } catch (IOException ignored) {
            return values;
        }
        return values;
    }

    private static String firstNonBlank(String... candidates) {
        for (String candidate : candidates) {
            if (candidate != null && !candidate.isBlank()) {
                return candidate;
            }
        }
        return "";
    }
}
