package com.sukinema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "app.admin-email=admin@sukinema.test",
        "spring.datasource.url=jdbc:h2:mem:catalogapi;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
})
@AutoConfigureMockMvc
class CatalogApiTests {

    private static String adminToken;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void signIn() throws Exception {
        if (adminToken != null) {
            return;
        }
        String response = mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Admin", "email", "admin@sukinema.test", "password", "contraseña-segura-1"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        adminToken = "Bearer " + objectMapper.readTree(response).get("token").asText();
    }

    private void createMovie(String title, String category) throws Exception {
        mockMvc.perform(post("/api/movies").header("Authorization", adminToken).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", title, "trailerUrl", "https://youtu.be/zSWdZVtXT7E", "category", category))))
                .andExpect(status().isCreated());
    }

    @Test
    void missingItemsAnswerWithAMessage() throws Exception {
        mockMvc.perform(get("/api/movies/999999").header("Authorization", adminToken).header("Accept-Language", "en"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Trailer not found."));
        mockMvc.perform(delete("/api/movies/999999").header("Authorization", adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Tráiler no encontrado."));
        mockMvc.perform(put("/api/profiles/999999").header("Authorization", adminToken).header("Accept-Language", "ca")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Nadie\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("No s'ha trobat el perfil."));
    }

    @Test
    void categoriesDifferingOnlyInCaseShareARow() throws Exception {
        createMovie("Primera de miedo", "Terror Nocturno");
        createMovie("Segunda de miedo", "terror nocturno");
        String response = mockMvc.perform(get("/api/movies/categories").header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        JsonNode rows = objectMapper.readTree(response);
        assertEquals(2, rows.get("Terror Nocturno").size());
        assertFalse(rows.has("terror nocturno"));
        assertEquals("Tendencias de Hoy", rows.fieldNames().next());
    }
}
