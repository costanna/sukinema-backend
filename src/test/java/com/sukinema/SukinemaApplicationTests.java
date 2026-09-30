package com.sukinema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sukinema.model.Movie;
import com.sukinema.repository.AccountRepository;
import com.sukinema.repository.MovieRepository;
import com.sukinema.repository.UserProfileRepository;
import com.sukinema.service.MovieService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "app.admin-email=admin@sukinema.test")
@AutoConfigureMockMvc
class SukinemaApplicationTests {

    private static final String PASSWORD = "contraseña-segura-1";
    private static final String YOUTUBE_URL = "https://youtu.be/zSWdZVtXT7E";
    private static final Map<String, String> TOKENS = new ConcurrentHashMap<>();

    @Autowired
    private MovieService movieService;

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // ---------- utilidades ----------

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private String body(Map<String, Object> fields) throws Exception {
        return objectMapper.writeValueAsString(fields);
    }

    private MvcResult register(String name, String email, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(body(Map.of("name", name, "email", email, "password", password)))).andReturn();
    }

    /** Token de una cuenta de prueba; la registra la primera vez que se pide. */
    private String tokenFor(String name, String email) throws Exception {
        String cached = TOKENS.get(email);
        if (cached != null) {
            return cached;
        }
        MvcResult result = register(name, email, PASSWORD);
        assertEquals(201, result.getResponse().getStatus(), "registro de " + email);
        String token = json(result).get("token").asText();
        TOKENS.put(email, token);
        return token;
    }

    private String adminToken() throws Exception {
        return tokenFor("Admin", "admin@sukinema.test");
    }

    private String userToken() throws Exception {
        return tokenFor("Usuaria", "usuaria@sukinema.test");
    }

    private static RequestPostProcessor auth(String token) {
        return request -> {
            request.addHeader("Authorization", "Bearer " + token);
            return request;
        };
    }

    private long firstProfileId(String token) throws Exception {
        return json(mockMvc.perform(get("/api/profiles").with(auth(token))).andReturn()).get(0).get("id").asLong();
    }

    private String libraryPath(String token, String collection, long movieId) throws Exception {
        return "/api/profiles/" + firstProfileId(token) + "/" + collection + "/" + movieId;
    }

    private long createMovie(String title, boolean featured) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/movies").with(auth(adminToken())).contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("title", title, "trailerUrl", YOUTUBE_URL, "featured", featured))))
                .andExpect(status().isCreated())
                .andReturn();
        return json(result).get("id").asLong();
    }

    // ---------- datos iniciales ----------

    @Test
    void contextLoads() {
        assertNotNull(movieService);
        assertNotNull(movieRepository);
    }

    @Test
    void testSampleDataLoaded() {
        List<Movie> movies = movieService.getAllMovies();
        assertTrue(movies.size() >= 10, "Debe haber al menos 10 tráilers precargados");
    }

    @Test
    void testFeaturedMovie() {
        Movie featured = movieService.getFeaturedMovie();
        assertNotNull(featured, "Debe existir un tráiler destacado");
        assertFalse(featured.getYoutubeId().isEmpty(), "El tráiler destacado debe tener un ID de YouTube");
    }

    @Test
    void testCategoriesGrouping() {
        Map<String, List<Movie>> grouped = movieService.getMoviesGroupedByCategory();
        assertFalse(grouped.isEmpty(), "Las categorías no deben estar vacías");
        assertTrue(grouped.containsKey("Tendencias de Hoy") || grouped.containsKey("Ciencia Ficción y Fantasía"));
    }

    @Test
    void testYoutubeIdExtraction() {
        assertEquals("Way9Dexny3w", Movie.extractYoutubeId("https://www.youtube.com/watch?v=Way9Dexny3w"));
        assertEquals("Way9Dexny3w", Movie.extractYoutubeId("https://youtu.be/Way9Dexny3w"));
        assertEquals("Way9Dexny3w", Movie.extractYoutubeId("https://www.youtube.com/embed/Way9Dexny3w"));
        assertEquals("Way9Dexny3w", Movie.extractYoutubeId("Way9Dexny3w"));
        assertEquals("Way9Dexny3w", Movie.extractYoutubeId("https://m.youtube.com/watch?app=desktop&v=Way9Dexny3w&t=10s"));
        assertEquals("Way9Dexny3w", Movie.extractYoutubeId("https://www.youtube.com/shorts/Way9Dexny3w"));
        assertEquals("Way9Dexny3w", Movie.extractYoutubeId("  https://youtu.be/Way9Dexny3w?si=abc  "));
        assertEquals("", Movie.extractYoutubeId("https://example.com/video.mp4"));
        assertEquals("", Movie.extractYoutubeId(null));
    }

    // ---------- acceso ----------

    @Test
    void testRegisterReturnsSessionWithoutPassword() throws Exception {
        MvcResult result = register("Nueva Cuenta", "  Nueva@Sukinema.Test ", PASSWORD);
        assertEquals(201, result.getResponse().getStatus());
        JsonNode response = json(result);
        assertFalse(response.get("token").asText().isBlank());
        assertEquals("nueva@sukinema.test", response.get("account").get("email").asText(), "el correo se normaliza");
        assertEquals("Nueva Cuenta", response.get("account").get("name").asText());
        assertEquals("USER", response.get("account").get("role").asText());
        String raw = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertFalse(raw.toLowerCase().contains("password"), "la respuesta no debe incluir la contraseña ni su hash");
        assertFalse(raw.toLowerCase().contains("hash"), "ni el hash del código de recuperación");
        assertTrue(response.get("recoveryCode").asText().matches("[A-Z2-9]{4}-[A-Z2-9]{4}-[A-Z2-9]{4}"));

        mockMvc.perform(get("/api/auth/me").with(auth(response.get("token").asText())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("nueva@sukinema.test"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        String hash = accountRepository.findByEmail("nueva@sukinema.test").orElseThrow().getPasswordHash();
        assertTrue(hash.startsWith("$2"), "la contraseña se guarda cifrada con BCrypt");
        assertNotEquals(PASSWORD, hash);
    }

    @Test
    void testRegisterValidation() throws Exception {
        tokenFor("Duplicada", "duplicada@sukinema.test");
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("name", "Otra", "email", "DUPLICADA@sukinema.test", "password", PASSWORD))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Ya existe una cuenta con ese correo."));

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("name", "Corta", "email", "corta@sukinema.test", "password", "1234567"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La contraseña debe tener entre 8 y 72 caracteres"));

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("name", "Mal correo", "email", "no-es-un-correo", "password", PASSWORD))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El correo no tiene un formato válido"));

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("{esto no es json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void testLogin() throws Exception {
        tokenFor("Entra", "entra@sukinema.test");

        MvcResult ok = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("email", " ENTRA@sukinema.test", "password", PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.account.name").value("Entra"))
                .andReturn();
        mockMvc.perform(get("/api/profiles").with(auth(json(ok).get("token").asText()))).andExpect(status().isOk());

        String wrongPassword = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("email", "entra@sukinema.test", "password", "otra-contraseña"))))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String unknownEmail = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("email", "nadie@sukinema.test", "password", PASSWORD))))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertEquals(wrongPassword, unknownEmail, "no se distingue entre correo desconocido y contraseña incorrecta");
    }

    @Test
    void testLoginLocksAfterRepeatedFailures() throws Exception {
        tokenFor("Bloqueo", "bloqueo@sukinema.test");
        String wrong = body(Map.of("email", "bloqueo@sukinema.test", "password", "incorrecta-1"));
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(wrong))
                    .andExpect(status().isUnauthorized());
        }
        // Bloqueado: ni siquiera la contraseña correcta entra hasta que pase el tiempo
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("email", "bloqueo@sukinema.test", "password", PASSWORD))))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message").value(containsString("Demasiados intentos")));
    }

    @Test
    void testApiRequiresSession() throws Exception {
        for (String path : List.of("/api/movies", "/api/movies/featured", "/api/movies/search?query=dune", "/api/profiles", "/api/auth/me")) {
            mockMvc.perform(get(path))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Inicia sesión para continuar."));
        }
        mockMvc.perform(put("/api/profiles/1/likes/1")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/profiles/1/library")).andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/movies/1")).andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/movies").header("Authorization", "Bearer no.es.un-token")).andExpect(status().isUnauthorized());
        String token = userToken();
        String tampered = token.substring(0, token.length() - 3) + (token.endsWith("AAA") ? "BBB" : "AAA");
        mockMvc.perform(get("/api/movies").with(auth(tampered))).andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
        mockMvc.perform(get("/api/movies").with(auth(token))).andExpect(status().isOk());
    }

    @Test
    void testDeletedAccountTokenStopsWorking() throws Exception {
        MvcResult result = register("Efímera", "efimera@sukinema.test", PASSWORD);
        String token = json(result).get("token").asText();
        long accountId = json(result).get("account").get("id").asLong();
        mockMvc.perform(get("/api/auth/me").with(auth(token))).andExpect(status().isOk());

        userProfileRepository.deleteAll(userProfileRepository.findByAccountIdOrderByCreatedAtAscIdAsc(accountId));
        accountRepository.deleteById(accountId);

        mockMvc.perform(get("/api/auth/me").with(auth(token))).andExpect(status().isUnauthorized());
    }

    @Test
    void testOnlyAdminCanChangeCatalog() throws Exception {
        mockMvc.perform(get("/api/auth/me").with(auth(adminToken()))).andExpect(jsonPath("$.role").value("ADMIN"));
        mockMvc.perform(get("/api/auth/me").with(auth(userToken()))).andExpect(jsonPath("$.role").value("USER"));

        long id = createMovie("Solo admin", false);
        String payload = body(Map.of("title", "Intento", "trailerUrl", YOUTUBE_URL));

        mockMvc.perform(post("/api/movies").with(auth(userToken())).contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(containsString("administradora")));
        mockMvc.perform(put("/api/movies/" + id).with(auth(userToken())).contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/movies/" + id).with(auth(userToken()))).andExpect(status().isForbidden());

        // Ver el catálogo y dar likes sí está al alcance de cualquier cuenta
        mockMvc.perform(get("/api/movies/" + id).with(auth(userToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Solo admin"));
        mockMvc.perform(put(libraryPath(userToken(), "likes", id)).with(auth(userToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likes").value(1));

        mockMvc.perform(delete("/api/movies/" + id).with(auth(adminToken()))).andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/movies/" + id).with(auth(adminToken()))).andExpect(status().isNotFound());
    }

    // ---------- perfiles ----------

    @Test
    void testNewAccountStartsWithOneProfile() throws Exception {
        String token = tokenFor("Un nombre de cuenta bastante largo", "larga@sukinema.test");
        mockMvc.perform(get("/api/profiles").with(auth(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Un nombre de cuenta"))
                .andExpect(jsonPath("$[0].avatar").isNotEmpty())
                .andExpect(jsonPath("$[0].isKid").value(false))
                .andExpect(jsonPath("$[0].kid").doesNotExist())
                .andExpect(jsonPath("$[0].account").doesNotExist());
    }

    @Test
    void testProfilesAreIsolatedBetweenAccounts() throws Exception {
        String owner = tokenFor("Dueña", "duena@sukinema.test");
        String other = tokenFor("Ajena", "ajena@sukinema.test");
        long ownerProfile = json(mockMvc.perform(get("/api/profiles").with(auth(owner))).andReturn()).get(0).get("id").asLong();

        mockMvc.perform(get("/api/profiles").with(auth(other)))
                .andExpect(jsonPath("$[*].id", not(hasItem((int) ownerProfile))));
        mockMvc.perform(get("/api/profiles/" + ownerProfile).with(auth(other))).andExpect(status().isNotFound());
        mockMvc.perform(put("/api/profiles/" + ownerProfile).with(auth(other)).contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("name", "Secuestrado"))))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/profiles/" + ownerProfile).with(auth(other))).andExpect(status().isNotFound());

        // Enviar el id de un perfil ajeno al crear no lo sobrescribe: crea uno nuevo en la cuenta propia
        mockMvc.perform(post("/api/profiles").with(auth(other)).contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("id", ownerProfile, "name", "Intruso"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", not((int) ownerProfile)));
        mockMvc.perform(get("/api/profiles/" + ownerProfile).with(auth(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Dueña"));
    }

    @Test
    void testProfileCrudAndLimits() throws Exception {
        String token = tokenFor("Familia", "familia@sukinema.test");

        MvcResult created = mockMvc.perform(post("/api/profiles").with(auth(token)).contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("name", "  Peques ", "avatar", "🦄", "color", "from-emerald-500 to-teal-700", "isKid", true))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Peques"))
                .andExpect(jsonPath("$.isKid").value(true))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andReturn();
        long id = json(created).get("id").asLong();

        mockMvc.perform(post("/api/profiles").with(auth(token)).contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("name", "PEQUES"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Ya existe un perfil con ese nombre."));
        mockMvc.perform(post("/api/profiles").with(auth(token)).contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("name", " "))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El nombre de perfil es obligatorio"));
        mockMvc.perform(post("/api/profiles").with(auth(token)).contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("name", "Un nombre de perfil demasiado largo"))))
                .andExpect(status().isBadRequest());

        // Editar: mismo nombre permitido; avatar y color se conservan si no se envían
        mockMvc.perform(put("/api/profiles/" + id).with(auth(token)).contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("name", "Peques", "isKid", false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatar").value("🦄"))
                .andExpect(jsonPath("$.color").value("from-emerald-500 to-teal-700"))
                .andExpect(jsonPath("$.isKid").value(false));
        mockMvc.perform(put("/api/profiles/" + id).with(auth(token)).contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("name", "familia"))))
                .andExpect(status().isConflict());
        mockMvc.perform(put("/api/profiles/999999").with(auth(token)).contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("name", "Nadie"))))
                .andExpect(status().isNotFound());

        // Máximo 5 perfiles por cuenta
        for (int i = 3; i <= 5; i++) {
            mockMvc.perform(post("/api/profiles").with(auth(token)).contentType(MediaType.APPLICATION_JSON)
                    .content(body(Map.of("name", "Perfil " + i)))).andExpect(status().isCreated());
        }
        mockMvc.perform(post("/api/profiles").with(auth(token)).contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("name", "El sexto"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("máximo 5")));

        // Siempre queda al menos un perfil
        JsonNode all = json(mockMvc.perform(get("/api/profiles").with(auth(token))).andReturn());
        assertEquals(5, all.size());
        for (int i = 0; i < 4; i++) {
            mockMvc.perform(delete("/api/profiles/" + all.get(i).get("id").asLong()).with(auth(token)))
                    .andExpect(status().isNoContent());
        }
        mockMvc.perform(delete("/api/profiles/" + all.get(4).get("id").asLong()).with(auth(token)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("al menos un perfil")));
    }

    // ---------- catálogo ----------

    @Test
    void testCreateMovieIgnoresServerOwnedFields() throws Exception {
        Movie victim = movieService.getAllMovies().get(3);

        MvcResult result = mockMvc.perform(post("/api/movies").with(auth(adminToken())).contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("id", victim.getId(), "title", "No sobrescribe", "trailerUrl", YOUTUBE_URL,
                                "likes", 99999, "youtubeId", "falso", "createdAt", "2001-01-01T00:00:00"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", not(victim.getId().intValue())))
                .andExpect(jsonPath("$.likes").value(0))
                .andExpect(jsonPath("$.youtubeId").value("zSWdZVtXT7E"))
                .andExpect(jsonPath("$.createdAt", not(startsWith("2001"))))
                .andReturn();

        assertEquals(victim.getTitle(), movieService.getMovieById(victim.getId()).orElseThrow().getTitle());
        movieService.deleteMovie(json(result).get("id").asLong());
    }

    @Test
    void testMovieValidationMessages() throws Exception {
        mockMvc.perform(post("/api/movies").with(auth(adminToken())).contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("title", "", "trailerUrl", YOUTUBE_URL))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El título es obligatorio"));

        mockMvc.perform(post("/api/movies").with(auth(adminToken())).contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("title", "Reparto largo", "trailerUrl", YOUTUBE_URL, "cast", "Actor con nombre largo, ".repeat(15)))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El reparto no puede superar los 255 caracteres"));

        mockMvc.perform(post("/api/movies").with(auth(adminToken())).contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("title", "No es YouTube", "trailerUrl", "https://example.com/video.mp4"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La URL del tráiler debe ser un enlace o un ID de vídeo de YouTube"));

        long id = createMovie("Válida", false);
        mockMvc.perform(put("/api/movies/" + id).with(auth(adminToken())).contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("title", "Válida", "trailerUrl", "https://vimeo.com/123456"))))
                .andExpect(status().isBadRequest());
        movieService.deleteMovie(id);
    }

    @Test
    void testOnlyOneFeaturedMovie() throws Exception {
        long first = createMovie("Destacado uno", true);
        mockMvc.perform(get("/api/movies/featured").with(auth(userToken()))).andExpect(jsonPath("$.id").value(first));
        assertEquals(1, movieRepository.findByFeaturedTrue().size(), "al destacar uno se desmarca el anterior");

        long second = createMovie("Destacado dos", false);
        mockMvc.perform(put("/api/movies/" + second).with(auth(adminToken())).contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("title", "Destacado dos", "trailerUrl", YOUTUBE_URL, "featured", true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.featured").value(true));
        mockMvc.perform(get("/api/movies/featured").with(auth(userToken()))).andExpect(jsonPath("$.id").value(second));
        assertEquals(1, movieRepository.findByFeaturedTrue().size());
        assertFalse(movieService.getMovieById(first).orElseThrow().isFeatured());
    }

    @Test
    void testUpdateMovieApi() throws Exception {
        long id = createMovie("Antes de editar", false);
        mockMvc.perform(put(libraryPath(userToken(), "likes", id)).with(auth(userToken()))).andExpect(status().isOk());

        String payload = body(Map.of("title", "Título editado", "trailerUrl", "https://youtu.be/Way9Dexny3w",
                "category", "Drama", "trending", true, "likes", 500));
        mockMvc.perform(put("/api/movies/" + id).with(auth(adminToken())).contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.title").value("Título editado"))
                .andExpect(jsonPath("$.youtubeId").value("Way9Dexny3w"))
                .andExpect(jsonPath("$.category").value("Drama"))
                .andExpect(jsonPath("$.likes").value(1));

        mockMvc.perform(put("/api/movies/999999").with(auth(adminToken())).contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isNotFound());
        movieService.deleteMovie(id);
    }

    @Test
    void testLikesAreOncePerProfile() throws Exception {
        long id = createMovie("Con likes", false);
        String user = userToken();
        String other = tokenFor("Otra fan", "otrafan@sukinema.test");

        // Repetir el like del mismo perfil no vuelve a sumar
        for (int attempt = 0; attempt < 3; attempt++) {
            mockMvc.perform(put(libraryPath(user, "likes", id)).with(auth(user)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.likes").value(1))
                    .andExpect(jsonPath("$.title").value("Con likes"));
        }
        mockMvc.perform(put(libraryPath(other, "likes", id)).with(auth(other)))
                .andExpect(jsonPath("$.likes").value(2));
        mockMvc.perform(get("/api/profiles/" + firstProfileId(user) + "/library").with(auth(user)))
                .andExpect(jsonPath("$.likes", hasItem((int) id)));

        // Quitar el like resta una sola vez
        mockMvc.perform(delete(libraryPath(user, "likes", id)).with(auth(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likes").value(1));
        mockMvc.perform(delete(libraryPath(user, "likes", id)).with(auth(user)))
                .andExpect(jsonPath("$.likes").value(1));
        mockMvc.perform(get("/api/profiles/" + firstProfileId(user) + "/library").with(auth(user)))
                .andExpect(jsonPath("$.likes", not(hasItem((int) id))));

        mockMvc.perform(put(libraryPath(user, "likes", 999999)).with(auth(user)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Tráiler no encontrado."));
        // El contador libre de antes ya no existe
        mockMvc.perform(post("/api/movies/" + id + "/like").with(auth(user))).andExpect(status().is4xxClientError());
        assertEquals(1, movieService.getMovieById(id).orElseThrow().getLikes());
        movieService.deleteMovie(id);
    }

    @Test
    void testMyListIsStoredPerProfile() throws Exception {
        String token = tokenFor("Coleccionista", "coleccionista@sukinema.test");
        String other = tokenFor("Curiosa", "curiosa@sukinema.test");
        long profile = firstProfileId(token);
        long first = createMovie("Para la lista 1", false);
        long second = createMovie("Para la lista 2", false);

        mockMvc.perform(get("/api/profiles/" + profile + "/library").with(auth(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.myList.length()").value(0))
                .andExpect(jsonPath("$.likes.length()").value(0));

        mockMvc.perform(put(libraryPath(token, "my-list", first)).with(auth(token))).andExpect(status().isNoContent());
        mockMvc.perform(put(libraryPath(token, "my-list", first)).with(auth(token))).andExpect(status().isNoContent());
        mockMvc.perform(put(libraryPath(token, "my-list", second)).with(auth(token))).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/profiles/" + profile + "/library").with(auth(token)))
                .andExpect(jsonPath("$.myList.length()").value(2))
                .andExpect(jsonPath("$.myList", hasItems((int) first, (int) second)));

        mockMvc.perform(delete(libraryPath(token, "my-list", first)).with(auth(token))).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/profiles/" + profile + "/library").with(auth(token)))
                .andExpect(jsonPath("$.myList.length()").value(1));

        mockMvc.perform(put(libraryPath(token, "my-list", 999999)).with(auth(token))).andExpect(status().isNotFound());

        // Otra cuenta no puede leer ni tocar la lista de este perfil
        mockMvc.perform(get("/api/profiles/" + profile + "/library").with(auth(other)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Perfil no encontrado."));
        mockMvc.perform(put("/api/profiles/" + profile + "/my-list/" + first).with(auth(other))).andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/profiles/" + profile + "/my-list/" + second).with(auth(other))).andExpect(status().isNotFound());
        mockMvc.perform(put("/api/profiles/" + profile + "/likes/" + first).with(auth(other))).andExpect(status().isNotFound());
        assertEquals(0, movieService.getMovieById(first).orElseThrow().getLikes());

        // Al eliminar un tráiler desaparece de las listas y de los likes
        mockMvc.perform(put(libraryPath(token, "likes", second)).with(auth(token))).andExpect(status().isOk());
        mockMvc.perform(delete("/api/movies/" + second).with(auth(adminToken()))).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/profiles/" + profile + "/library").with(auth(token)))
                .andExpect(jsonPath("$.myList.length()").value(0))
                .andExpect(jsonPath("$.likes.length()").value(0));
        movieService.deleteMovie(first);
    }

    @Test
    void testDeletingProfileRemovesItsLibrary() throws Exception {
        String token = tokenFor("Temporal", "temporal@sukinema.test");
        long movie = createMovie("En un perfil que se borra", false);
        long extra = json(mockMvc.perform(post("/api/profiles").with(auth(token)).contentType(MediaType.APPLICATION_JSON)
                .content(body(Map.of("name", "Efímero")))).andExpect(status().isCreated()).andReturn()).get("id").asLong();

        mockMvc.perform(put("/api/profiles/" + extra + "/my-list/" + movie).with(auth(token))).andExpect(status().isNoContent());
        mockMvc.perform(put("/api/profiles/" + extra + "/likes/" + movie).with(auth(token))).andExpect(status().isOk());
        mockMvc.perform(delete("/api/profiles/" + extra).with(auth(token))).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/profiles/" + extra + "/library").with(auth(token))).andExpect(status().isNotFound());

        // El tráiler sigue existiendo y se puede eliminar sin restos del perfil borrado
        mockMvc.perform(delete("/api/movies/" + movie).with(auth(adminToken()))).andExpect(status().isNoContent());
    }

    // ---------- contraseñas ----------

    @Test
    void testChangePasswordClosesOtherSessions() throws Exception {
        MvcResult registered = register("Cambia", "cambia@sukinema.test", PASSWORD);
        String oldToken = json(registered).get("token").asText();
        String newPassword = "otra-contraseña-2";
        // Los tokens llevan precisión de segundos: se espera para que el nuevo sea posterior al antiguo
        Thread.sleep(1100);

        mockMvc.perform(post("/api/auth/password").with(auth(oldToken)).contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("currentPassword", "no-es-esta-1", "newPassword", newPassword))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("La contraseña actual no es correcta."));
        mockMvc.perform(post("/api/auth/password").with(auth(oldToken)).contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("currentPassword", PASSWORD, "newPassword", "corta"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La contraseña debe tener entre 8 y 72 caracteres"));
        mockMvc.perform(get("/api/auth/me").with(auth(oldToken))).andExpect(status().isOk());

        MvcResult changed = mockMvc.perform(post("/api/auth/password").with(auth(oldToken)).contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("currentPassword", PASSWORD, "newPassword", newPassword))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recoveryCode").doesNotExist())
                .andReturn();
        String newToken = json(changed).get("token").asText();

        mockMvc.perform(get("/api/auth/me").with(auth(oldToken))).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/auth/me").with(auth(newToken))).andExpect(status().isOk());
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("email", "cambia@sukinema.test", "password", PASSWORD))))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("email", "cambia@sukinema.test", "password", newPassword))))
                .andExpect(status().isOk());
    }

    @Test
    void testRecoverPasswordWithRecoveryCode() throws Exception {
        MvcResult registered = register("Olvidadiza", "olvidadiza@sukinema.test", PASSWORD);
        String recoveryCode = json(registered).get("recoveryCode").asText();
        String oldToken = json(registered).get("token").asText();
        assertTrue(recoveryCode.matches("[A-Z2-9]{4}-[A-Z2-9]{4}-[A-Z2-9]{4}"), recoveryCode);
        String stored = accountRepository.findByEmail("olvidadiza@sukinema.test").orElseThrow().getRecoveryCodeHash();
        assertTrue(stored.startsWith("$2") && !stored.contains(recoveryCode.replace("-", "")), "el código se guarda cifrado");
        Thread.sleep(1100);

        String wrongCode = mockMvc.perform(post("/api/auth/recover").contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("email", "olvidadiza@sukinema.test", "recoveryCode", "AAAA-BBBB-CCCC", "newPassword", "nueva-contraseña-3"))))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String unknownEmail = mockMvc.perform(post("/api/auth/recover").contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("email", "nadie-mas@sukinema.test", "recoveryCode", recoveryCode, "newPassword", "nueva-contraseña-3"))))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertEquals(wrongCode, unknownEmail, "no se revela si el correo existe");

        // El código vale escrito en minúsculas, con espacios o sin guiones
        String typed = " " + recoveryCode.toLowerCase().replace("-", " ") + " ";
        MvcResult recovered = mockMvc.perform(post("/api/auth/recover").contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("email", " OLVIDADIZA@sukinema.test", "recoveryCode", typed, "newPassword", "nueva-contraseña-3"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.account.email").value("olvidadiza@sukinema.test"))
                .andReturn();
        String newCode = json(recovered).get("recoveryCode").asText();
        assertNotEquals(recoveryCode, newCode, "el código es de un solo uso");

        mockMvc.perform(get("/api/auth/me").with(auth(oldToken))).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/auth/me").with(auth(json(recovered).get("token").asText()))).andExpect(status().isOk());
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("email", "olvidadiza@sukinema.test", "password", "nueva-contraseña-3"))))
                .andExpect(status().isOk());
        // El código usado ya no sirve
        mockMvc.perform(post("/api/auth/recover").contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("email", "olvidadiza@sukinema.test", "recoveryCode", recoveryCode, "newPassword", "otra-más-segura-4"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testRecoveryAttemptsAreLimited() throws Exception {
        register("Atacada", "atacada@sukinema.test", PASSWORD);
        String attempt = body(Map.of("email", "atacada@sukinema.test", "recoveryCode", "ZZZZ-ZZZZ-ZZZZ", "newPassword", "clave-del-atacante-1"));
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/auth/recover").contentType(MediaType.APPLICATION_JSON).content(attempt))
                    .andExpect(status().isUnauthorized());
        }
        mockMvc.perform(post("/api/auth/recover").contentType(MediaType.APPLICATION_JSON).content(attempt))
                .andExpect(status().isTooManyRequests());
        // Probar códigos no bloquea el inicio de sesión de la dueña de la cuenta
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("email", "atacada@sukinema.test", "password", PASSWORD))))
                .andExpect(status().isOk());
    }

    @Test
    void testRegenerateRecoveryCode() throws Exception {
        MvcResult registered = register("Precavida", "precavida@sukinema.test", PASSWORD);
        String token = json(registered).get("token").asText();
        String firstCode = json(registered).get("recoveryCode").asText();

        mockMvc.perform(post("/api/auth/recovery-code").with(auth(token)).contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("password", "no-es-la-mía-9"))))
                .andExpect(status().isForbidden());
        MvcResult regenerated = mockMvc.perform(post("/api/auth/recovery-code").with(auth(token)).contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("password", PASSWORD))))
                .andExpect(status().isOk())
                .andReturn();
        String secondCode = json(regenerated).get("recoveryCode").asText();
        assertNotEquals(firstCode, secondCode);
        mockMvc.perform(get("/api/auth/me").with(auth(token))).andExpect(status().isOk());

        // Solo vale el código más reciente
        mockMvc.perform(post("/api/auth/recover").contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("email", "precavida@sukinema.test", "recoveryCode", firstCode, "newPassword", "otra-contraseña-5"))))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/auth/recover").contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("email", "precavida@sukinema.test", "recoveryCode", secondCode, "newPassword", "otra-contraseña-5"))))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body(Map.of("email", "nadie@x.test", "password", "x"))))
                .andExpect(jsonPath("$.recoveryCode").doesNotExist());
    }

    @Test
    void testSearch() throws Exception {
        mockMvc.perform(get("/api/movies/search").param("query", "NOLAN").with(auth(userToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[*].director", everyItem(is("Christopher Nolan"))));

        mockMvc.perform(get("/api/movies/search").param("query", "director que no existe").with(auth(userToken())))
                .andExpect(jsonPath("$.length()").value(0));

        // % y _ se buscan como texto, no como comodines
        mockMvc.perform(get("/api/movies/search").param("query", "%").with(auth(userToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(get("/api/movies/search").param("query", "_").with(auth(userToken())))
                .andExpect(jsonPath("$.length()").value(0));

        long id = createMovie("100% real_mente", false);
        mockMvc.perform(get("/api/movies/search").param("query", "100%").with(auth(userToken())))
                .andExpect(jsonPath("$.length()").value(1));
        mockMvc.perform(get("/api/movies/search").param("query", "real_").with(auth(userToken())))
                .andExpect(jsonPath("$.length()").value(1));
        movieService.deleteMovie(id);
    }

    // ---------- CORS ----------

    @Test
    void testCorsWithSession() throws Exception {
        // La comprobación previa del navegador no lleva token y debe pasar
        mockMvc.perform(options("/api/movies/1")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "PUT")
                        .header("Access-Control-Request-Headers", "authorization,content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));

        // Un 401 también lleva las cabeceras CORS: si no, el navegador no deja leerlo al frontend
        mockMvc.perform(get("/api/movies").header("Origin", "http://localhost:5173"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));

        mockMvc.perform(get("/api/movies").header("Origin", "http://localhost:5173").with(auth(userToken())))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }
}
