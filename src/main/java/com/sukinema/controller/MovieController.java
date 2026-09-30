package com.sukinema.controller;

import com.sukinema.auth.AuthInterceptor;
import com.sukinema.exception.ApiException;
import com.sukinema.model.Account;
import com.sukinema.model.Movie;
import com.sukinema.service.MovieService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/movies")
public class MovieController {

    private final MovieService movieService;

    public MovieController(MovieService movieService) {
        this.movieService = movieService;
    }

    @GetMapping
    public ResponseEntity<List<Movie>> getAllMovies() {
        return ResponseEntity.ok(movieService.getAllMovies());
    }

    @GetMapping("/featured")
    public ResponseEntity<Movie> getFeaturedMovie() {
        Movie featured = movieService.getFeaturedMovie();
        if (featured == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(featured);
    }

    @GetMapping("/categories")
    public ResponseEntity<Map<String, List<Movie>>> getMoviesGroupedByCategory() {
        return ResponseEntity.ok(movieService.getMoviesGroupedByCategory());
    }

    @GetMapping("/trending")
    public ResponseEntity<List<Movie>> getTrendingMovies() {
        return ResponseEntity.ok(movieService.getTrendingMovies());
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<Movie>> getByCategory(@PathVariable String category) {
        return ResponseEntity.ok(movieService.getMoviesByCategory(category));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Movie> getMovieById(@PathVariable Long id) {
        return movieService.getMovieById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    public ResponseEntity<List<Movie>> searchMovies(@RequestParam(required = false, defaultValue = "") String query) {
        return ResponseEntity.ok(movieService.searchMovies(query));
    }

    @PostMapping
    public ResponseEntity<Movie> createMovie(
            @RequestAttribute(AuthInterceptor.ACCOUNT_ATTRIBUTE) Account account,
            @Valid @RequestBody Movie movie) {
        requireAdmin(account);
        Movie saved = movieService.createMovie(movie);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Movie> updateMovie(
            @RequestAttribute(AuthInterceptor.ACCOUNT_ATTRIBUTE) Account account,
            @PathVariable Long id, @Valid @RequestBody Movie movie) {
        requireAdmin(account);
        return movieService.updateMovie(id, movie)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMovie(
            @RequestAttribute(AuthInterceptor.ACCOUNT_ATTRIBUTE) Account account, @PathVariable Long id) {
        requireAdmin(account);
        if (movieService.deleteMovie(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    // Cualquier cuenta ve el catálogo; modificarlo es cosa de la cuenta administradora
    private static void requireAdmin(Account account) {
        if (!account.isAdmin()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Solo la cuenta administradora puede modificar el catálogo.");
        }
    }
}
