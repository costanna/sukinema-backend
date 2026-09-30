package com.sukinema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Los mensajes de error llegan en el idioma de la cabecera Accept-Language (ca, en, es)
 * y en castellano si no se pide ninguno o se pide uno que no está traducido.
 */
@SpringBootTest(properties = {
        "app.admin-email=admin@sukinema.test",
        "spring.datasource.url=jdbc:h2:mem:localized;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
})
@AutoConfigureMockMvc
class LocalizedMessagesTests {

    private static final String PASSWORD = "contraseña-segura-1";
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\d+}");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String body(Map<String, Object> fields) throws Exception {
        return objectMapper.writeValueAsString(fields);
    }

    private MockHttpServletRequestBuilder in(String language, MockHttpServletRequestBuilder request) {
        return language == null ? request : request.header("Accept-Language", language);
    }

    private JsonNode register(String name, String email) throws Exception {
        String response = mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("name", name, "email", email, "password", PASSWORD))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(response);
    }

    private Properties bundle(String name) throws Exception {
        Properties properties = new Properties();
        try (InputStream in = getClass().getResourceAsStream("/" + name)) {
            assertNotNull(in, "falta " + name);
            properties.load(new InputStreamReader(in, StandardCharsets.UTF_8));
        }
        return properties;
    }

    private Set<String> placeholders(String text) {
        Set<String> found = new TreeSet<>();
        Matcher matcher = PLACEHOLDER.matcher(text);
        while (matcher.find()) {
            found.add(matcher.group());
        }
        return found;
    }

    @Test
    void sessionRequiredMessageFollowsRequestLanguage() throws Exception {
        mockMvc.perform(in(null, get("/api/movies")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Inicia sesión para continuar."));
        mockMvc.perform(in("ca", get("/api/movies")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Inicia la sessió per continuar."));
        mockMvc.perform(in("en", get("/api/movies")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Sign in to continue."));
    }

    @Test
    void browserLanguageHeadersAreUnderstood() throws Exception {
        // Así envía el idioma un navegador: con región y varias preferencias
        mockMvc.perform(in("en-GB,en;q=0.9,es;q=0.8", get("/api/movies")))
                .andExpect(jsonPath("$.message").value("Sign in to continue."));
        mockMvc.perform(in("ca-ES,ca;q=0.9", get("/api/movies")))
                .andExpect(jsonPath("$.message").value("Inicia la sessió per continuar."));
        // Un idioma sin traducción recibe el castellano
        mockMvc.perform(in("fr-FR,fr;q=0.9", get("/api/movies")))
                .andExpect(jsonPath("$.message").value("Inicia sesión para continuar."));
    }

    @Test
    void businessErrorsAreTranslated() throws Exception {
        String login = body(Map.of("email", "nadie@sukinema.test", "password", "no-es-la-clave"));
        mockMvc.perform(in("ca", post("/api/auth/login")).contentType(MediaType.APPLICATION_JSON).content(login))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Correu o contrasenya incorrectes."));
        mockMvc.perform(in("en", post("/api/auth/login")).contentType(MediaType.APPLICATION_JSON).content(login))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Incorrect email or password."));

        register("Repetida", "repetida@sukinema.test");
        String again = body(Map.of("name", "Repetida", "email", "repetida@sukinema.test", "password", PASSWORD));
        mockMvc.perform(in("en", post("/api/auth/register")).contentType(MediaType.APPLICATION_JSON).content(again))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("An account with that email already exists."));
    }

    @Test
    void validationMessagesAreTranslated() throws Exception {
        String shortPassword = body(Map.of("name", "Corta", "email", "corta@sukinema.test", "password", "1234"));
        mockMvc.perform(in(null, post("/api/auth/register")).contentType(MediaType.APPLICATION_JSON).content(shortPassword))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La contraseña debe tener entre 8 y 72 caracteres"));
        mockMvc.perform(in("ca", post("/api/auth/register")).contentType(MediaType.APPLICATION_JSON).content(shortPassword))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La contrasenya ha de tenir entre 8 i 72 caràcters"));
        mockMvc.perform(in("en", post("/api/auth/register")).contentType(MediaType.APPLICATION_JSON).content(shortPassword))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("The password must be between 8 and 72 characters long"));

        String token = register("Admin", "admin@sukinema.test").get("token").asText();
        String notYoutube = body(Map.of("title", "Sin tráiler", "trailerUrl", "https://example.com/video"));
        mockMvc.perform(in("en", post("/api/movies")).header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(notYoutube))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("The trailer URL must be a YouTube link or video ID"));
        mockMvc.perform(in("ca", post("/api/movies")).header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{ esto no es JSON"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El cos de la petició no és vàlid."));
    }

    @Test
    void messagesWithArgumentsAreTranslated() throws Exception {
        String token = register("Perfiles", "perfiles@sukinema.test").get("token").asText();
        String message = null;
        // Se crean perfiles hasta llegar al tope de la cuenta
        for (int i = 0; i < 20 && message == null; i++) {
            var response = mockMvc.perform(in("en", post("/api/profiles")).header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON).content(body(Map.of("name", "Perfil " + i))))
                    .andReturn().getResponse();
            if (response.getStatus() == 409) {
                message = objectMapper.readTree(response.getContentAsString(StandardCharsets.UTF_8)).get("message").asText();
            }
        }
        assertNotNull(message, "la cuenta debería tener un máximo de perfiles");
        assertTrue(message.matches("An account can have at most \\d+ profiles\\."), message);

        mockMvc.perform(in("ca", put("/api/profiles/999999/my-list/1")).header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("No s'ha trobat el perfil."));
    }

    @Test
    void everyLanguageHasTheSameMessages() throws Exception {
        Properties spanish = bundle("messages.properties");
        assertFalse(spanish.isEmpty());
        for (String name : new String[]{"messages_ca.properties", "messages_en.properties"}) {
            Properties translated = bundle(name);
            assertEquals(new TreeSet<>(spanish.stringPropertyNames()), new TreeSet<>(translated.stringPropertyNames()),
                    "claves de " + name);
            for (String key : spanish.stringPropertyNames()) {
                assertFalse(translated.getProperty(key).isBlank(), key + " vacío en " + name);
                assertEquals(placeholders(spanish.getProperty(key)), placeholders(translated.getProperty(key)),
                        "parámetros de " + key + " en " + name);
            }
        }
    }
}
