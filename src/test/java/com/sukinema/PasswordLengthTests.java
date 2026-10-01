package com.sukinema;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * BCrypt solo usa los primeros 72 bytes de la contraseña. Con acentos o emojis, 72 caracteres
 * pueden ocupar más: esas contraseñas se rechazan para que dos distintas no valgan lo mismo.
 */
@SpringBootTest(properties = {
        "app.admin-email=admin@sukinema.test",
        "spring.datasource.url=jdbc:h2:mem:passwordlength;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
})
@AutoConfigureMockMvc
class PasswordLengthTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String json(Map<String, Object> fields) throws Exception {
        return objectMapper.writeValueAsString(fields);
    }

    @Test
    void passwordsLongerThan72BytesAreRejected() throws Exception {
        String longPassword = "ñ".repeat(40);
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Larga", "email", "larga@sukinema.test", "password", longPassword))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La contraseña debe tener entre 8 y 72 caracteres"));
    }

    @Test
    void passwordsUpTo72BytesWork() throws Exception {
        String password = "ñ".repeat(36);
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Justa", "email", "justa@sukinema.test", "password", password))))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "justa@sukinema.test", "password", password))))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "justa@sukinema.test", "password", password + "abcd"))))
                .andExpect(status().isUnauthorized());
    }
}
