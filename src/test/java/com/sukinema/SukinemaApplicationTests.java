package com.sukinema;

import com.sukinema.model.Movie;
import com.sukinema.model.UserProfile;
import com.sukinema.repository.MovieRepository;
import com.sukinema.service.MovieService;
import com.sukinema.service.UserProfileService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class SukinemaApplicationTests {

    @Autowired
    private MovieService movieService;

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private UserProfileService userProfileService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void contextLoads() {
        assertNotNull(movieService);
        assertNotNull(movieRepository);
    }

    @Test
    void testSampleDataLoaded() {
        List<Movie> movies = movieService.getAllMovies();
        assertFalse(movies.isEmpty(), "Debe haber películas cargadas por DataInitializer");
        assertTrue(movies.size() >= 10, "Debe haber al menos 10 tráilers precargados");
    }

    @Test
    void testFeaturedMovie() {
        Movie featured = movieService.getFeaturedMovie();
        assertNotNull(featured, "Debe existir un tráiler destacado");
        assertNotNull(featured.getYoutubeId(), "El tráiler destacado debe tener un ID de YouTube");
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
    }

    @Test
    void testSampleProfilesLoaded() {
        List<UserProfile> profiles = userProfileService.getAllProfiles();
        assertTrue(profiles.size() >= 4, "Debe haber al menos 4 perfiles precargados");
        assertEquals("Anna", profiles.get(0).getName(), "Los perfiles deben venir en orden de creación");
        assertTrue(profiles.stream().anyMatch(UserProfile::isKid), "Debe existir un perfil infantil");
    }

    @Test
    void testProfilesApiExposesIsKid() throws Exception {
        mockMvc.perform(get("/api/profiles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[3].name").value("Kids"))
                .andExpect(jsonPath("$[3].isKid").value(true))
                .andExpect(jsonPath("$[3].kid").doesNotExist());
    }

    @Test
    void testCreateAndDeleteProfileApi() throws Exception {
        String body = "{\"name\":\"Invitado\",\"avatar\":\"👻\",\"color\":\"from-purple-600 to-fuchsia-800\",\"isKid\":true}";
        String response = mockMvc.perform(post("/api/profiles").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.isKid").value(true))
                .andReturn().getResponse().getContentAsString();
        String id = response.replaceAll(".*\"id\":(\\d+).*", "$1");

        mockMvc.perform(delete("/api/profiles/" + id)).andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/profiles/" + id)).andExpect(status().isNotFound());
    }

    @Test
    void testUpdateProfileApi() throws Exception {
        String created = mockMvc.perform(post("/api/profiles").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Temporal\",\"avatar\":\"🚀\",\"color\":\"from-pink-500 to-rose-600\",\"isKid\":false}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String id = created.replaceAll(".*\"id\":(\\d+).*", "$1");

        mockMvc.perform(put("/api/profiles/" + id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Renombrado\",\"avatar\":\"🐉\",\"color\":\"from-blue-600 to-indigo-800\",\"isKid\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(Integer.parseInt(id)))
                .andExpect(jsonPath("$.name").value("Renombrado"))
                .andExpect(jsonPath("$.avatar").value("🐉"))
                .andExpect(jsonPath("$.color").value("from-blue-600 to-indigo-800"))
                .andExpect(jsonPath("$.isKid").value(true))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());

        // Sin avatar ni color en la petición se conservan los que ya tenía
        mockMvc.perform(put("/api/profiles/" + id).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Solo nombre\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Solo nombre"))
                .andExpect(jsonPath("$.avatar").value("🐉"))
                .andExpect(jsonPath("$.color").value("from-blue-600 to-indigo-800"));

        mockMvc.perform(put("/api/profiles/" + id).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put("/api/profiles/999999").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Nadie\"}"))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/profiles/" + id)).andExpect(status().isNoContent());
    }

    @Test
    void testCreateProfileRequiresName() throws Exception {
        mockMvc.perform(post("/api/profiles").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\" \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testUpdateMovieApi() throws Exception {
        Movie movie = movieService.getAllMovies().get(1);
        String body = "{\"title\":\"Título editado\",\"trailerUrl\":\"https://youtu.be/zSWdZVtXT7E\",\"category\":\"Drama\",\"trending\":true}";

        mockMvc.perform(put("/api/movies/" + movie.getId()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(movie.getId()))
                .andExpect(jsonPath("$.title").value("Título editado"))
                .andExpect(jsonPath("$.youtubeId").value("zSWdZVtXT7E"))
                .andExpect(jsonPath("$.category").value("Drama"));

        mockMvc.perform(put("/api/movies/999999").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    void testLikeMovieApi() throws Exception {
        Movie movie = movieService.getAllMovies().get(0);
        int likesBefore = movie.getLikes();

        mockMvc.perform(post("/api/movies/" + movie.getId() + "/like"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likes").value(likesBefore + 1));

        mockMvc.perform(post("/api/movies/999999/like")).andExpect(status().isNotFound());
    }

    @Test
    void testCorsRequestFromFrontend() throws Exception {
        mockMvc.perform(get("/api/movies").header("Origin", "http://localhost:5173"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));

        mockMvc.perform(get("/api/profiles").header("Origin", "http://localhost:5173"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }
}
