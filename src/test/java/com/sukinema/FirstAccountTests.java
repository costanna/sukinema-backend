package com.sukinema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sukinema.model.UserProfile;
import com.sukinema.repository.UserProfileRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sin ADMIN_EMAIL, la primera cuenta que se registra administra el catálogo y hereda
 * los perfiles que existían antes de que hubiera cuentas. Base de datos propia para
 * que "la primera cuenta" no dependa del orden de otros tests.
 */
@SpringBootTest(properties = {
        "app.admin-email=",
        "spring.datasource.url=jdbc:h2:mem:firstaccount;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
})
@AutoConfigureMockMvc
class FirstAccountTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserProfileRepository userProfileRepository;

    private JsonNode register(String name, String email) throws Exception {
        String response = mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", name, "email", email, "password", "contraseña-segura-1"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(response);
    }

    @Test
    void firstAccountIsAdminAndInheritsExistingProfiles() throws Exception {
        // Perfil anterior a las cuentas, como el que ya existe en producción
        userProfileRepository.save(new UserProfile("Anna", "🦄", "from-pink-500 to-rose-600", false));

        JsonNode first = register("Primera", "primera@sukinema.test");
        assertEquals("ADMIN", first.get("account").get("role").asText());
        mockMvc.perform(get("/api/profiles").header("Authorization", "Bearer " + first.get("token").asText()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Anna"))
                .andExpect(jsonPath("$[0].avatar").value("🦄"));

        JsonNode second = register("Segunda", "segunda@sukinema.test");
        assertEquals("USER", second.get("account").get("role").asText());
        mockMvc.perform(get("/api/profiles").header("Authorization", "Bearer " + second.get("token").asText()))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Segunda"));
    }
}
