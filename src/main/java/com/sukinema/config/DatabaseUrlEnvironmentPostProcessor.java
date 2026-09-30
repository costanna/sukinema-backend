package com.sukinema.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.Profiles;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Permite pegar en DATABASE_URL la cadena de conexión tal como la da Neon
 * (postgresql://usuario:clave@host/db?sslmode=require) y la traduce a las
 * propiedades spring.datasource.* que espera Spring Boot. También acepta una URL JDBC.
 */
public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor {

    static final String PROPERTY_SOURCE_NAME = "sukinemaDatabaseUrl";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String databaseUrl = environment.getProperty("DATABASE_URL");
        if (databaseUrl == null || databaseUrl.isBlank()) {
            if (environment.acceptsProfiles(Profiles.of("prod")) && !environment.containsProperty("SPRING_DATASOURCE_URL")) {
                throw new IllegalStateException(
                        "Falta la variable de entorno DATABASE_URL: el perfil prod necesita la cadena de conexión de Neon.");
            }
            return;
        }
        environment.getPropertySources().addFirst(
                new MapPropertySource(PROPERTY_SOURCE_NAME, toDataSourceProperties(databaseUrl.trim())));
    }

    static Map<String, Object> toDataSourceProperties(String databaseUrl) {
        Map<String, Object> properties = new LinkedHashMap<>();

        if (databaseUrl.startsWith("jdbc:")) {
            properties.put("spring.datasource.url", databaseUrl);
            int queryStart = databaseUrl.indexOf('?');
            if (queryStart != -1) {
                for (String param : databaseUrl.substring(queryStart + 1).split("&")) {
                    if (param.startsWith("user=")) {
                        properties.put("spring.datasource.username", decode(param.substring("user=".length())));
                    } else if (param.startsWith("password=")) {
                        properties.put("spring.datasource.password", decode(param.substring("password=".length())));
                    }
                }
            }
            return properties;
        }

        URI uri;
        try {
            uri = URI.create(databaseUrl);
        } catch (IllegalArgumentException e) {
            // Sin incluir la URL en el mensaje: contiene la contraseña
            throw new IllegalStateException("DATABASE_URL no es una URL válida.");
        }
        String scheme = uri.getScheme();
        if (uri.getHost() == null || !("postgres".equals(scheme) || "postgresql".equals(scheme))) {
            throw new IllegalStateException(
                    "DATABASE_URL debe tener la forma postgresql://usuario:clave@host/basededatos o ser una URL jdbc:.");
        }

        StringBuilder jdbcUrl = new StringBuilder("jdbc:postgresql://").append(uri.getHost());
        if (uri.getPort() != -1) {
            jdbcUrl.append(':').append(uri.getPort());
        }
        jdbcUrl.append(uri.getRawPath() == null ? "" : uri.getRawPath());
        if (uri.getRawQuery() != null) {
            // channel_binding es un parámetro de libpq que el driver JDBC no usa
            String query = Arrays.stream(uri.getRawQuery().split("&"))
                    .filter(param -> !param.startsWith("channel_binding="))
                    .collect(Collectors.joining("&"));
            if (!query.isEmpty()) {
                jdbcUrl.append('?').append(query);
            }
        }
        properties.put("spring.datasource.url", jdbcUrl.toString());

        String userInfo = uri.getRawUserInfo();
        if (userInfo != null) {
            int separator = userInfo.indexOf(':');
            if (separator == -1) {
                properties.put("spring.datasource.username", decode(userInfo));
            } else {
                properties.put("spring.datasource.username", decode(userInfo.substring(0, separator)));
                properties.put("spring.datasource.password", decode(userInfo.substring(separator + 1)));
            }
        }
        return properties;
    }

    private static String decode(String value) {
        // URLDecoder convierte "+" en espacio; en una URL de conexión es un "+" literal
        return URLDecoder.decode(value.replace("+", "%2B"), StandardCharsets.UTF_8);
    }
}
