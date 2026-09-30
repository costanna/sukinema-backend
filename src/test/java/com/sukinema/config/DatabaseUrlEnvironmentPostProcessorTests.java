package com.sukinema.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.mock.env.MockEnvironment;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseUrlEnvironmentPostProcessorTests {

    private final DatabaseUrlEnvironmentPostProcessor postProcessor = new DatabaseUrlEnvironmentPostProcessor();

    @Test
    void convertsNeonConnectionString() {
        Map<String, Object> properties = DatabaseUrlEnvironmentPostProcessor.toDataSourceProperties(
                "postgresql://neondb_owner:npg_AbC123@ep-cool-name-a1b2c3-pooler.eu-central-1.aws.neon.tech/neondb?sslmode=require&channel_binding=require");

        assertEquals("jdbc:postgresql://ep-cool-name-a1b2c3-pooler.eu-central-1.aws.neon.tech/neondb?sslmode=require",
                properties.get("spring.datasource.url"));
        assertEquals("neondb_owner", properties.get("spring.datasource.username"));
        assertEquals("npg_AbC123", properties.get("spring.datasource.password"));
    }

    @Test
    void keepsPortAndDecodesCredentials() {
        Map<String, Object> properties = DatabaseUrlEnvironmentPostProcessor.toDataSourceProperties(
                "postgres://user%40mail:p%40ss+w%2Frd@localhost:5433/sukinema");

        assertEquals("jdbc:postgresql://localhost:5433/sukinema", properties.get("spring.datasource.url"));
        assertEquals("user@mail", properties.get("spring.datasource.username"));
        assertEquals("p@ss+w/rd", properties.get("spring.datasource.password"));
    }

    @Test
    void acceptsJdbcUrlWithCredentialsInQuery() {
        String jdbcUrl = "jdbc:postgresql://host.neon.tech/neondb?user=neondb_owner&password=npg_AbC123&sslmode=require";
        Map<String, Object> properties = DatabaseUrlEnvironmentPostProcessor.toDataSourceProperties(jdbcUrl);

        assertEquals(jdbcUrl, properties.get("spring.datasource.url"));
        assertEquals("neondb_owner", properties.get("spring.datasource.username"));
        assertEquals("npg_AbC123", properties.get("spring.datasource.password"));
    }

    @Test
    void rejectsOtherSchemesWithoutLeakingThePassword() {
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> DatabaseUrlEnvironmentPostProcessor.toDataSourceProperties("mysql://user:secreto@host/db"));
        assertFalse(error.getMessage().contains("secreto"));
    }

    @Test
    void databaseUrlOverridesDefaultDatasource() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("spring.datasource.url", "jdbc:h2:mem:sukinemadb")
                .withProperty("spring.datasource.username", "sa")
                .withProperty("DATABASE_URL", "postgresql://neon_user:neon_pass@host.neon.tech/neondb?sslmode=require");

        postProcessor.postProcessEnvironment(environment, new SpringApplication());

        assertEquals("jdbc:postgresql://host.neon.tech/neondb?sslmode=require", environment.getProperty("spring.datasource.url"));
        assertEquals("neon_user", environment.getProperty("spring.datasource.username"));
        assertEquals("neon_pass", environment.getProperty("spring.datasource.password"));
    }

    @Test
    void withoutDatabaseUrlKeepsLocalDatasource() {
        MockEnvironment environment = new MockEnvironment().withProperty("spring.datasource.url", "jdbc:h2:mem:sukinemadb");

        postProcessor.postProcessEnvironment(environment, new SpringApplication());

        assertEquals("jdbc:h2:mem:sukinemadb", environment.getProperty("spring.datasource.url"));
    }

    @Test
    void prodProfileFailsFastWithoutDatabaseUrl() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> postProcessor.postProcessEnvironment(environment, new SpringApplication()));
        assertTrue(error.getMessage().contains("DATABASE_URL"));
    }
}
