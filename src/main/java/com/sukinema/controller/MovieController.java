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
        return ResponseEntity.ok(movieService.getFeaturedMovie().orElseThrow(MovieController::notFound));
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
        return ResponseEntity.ok(movieService.getMovieById(id).orElseThrow(MovieController::notFound));
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
        return ResponseEntity.ok(movieService.updateMovie(id, movie).orElseThrow(MovieController::notFound));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMovie(
            @RequestAttribute(AuthInterceptor.ACCOUNT_ATTRIBUTE) Account account, @PathVariable Long id) {
        requireAdmin(account);
        if (!movieService.deleteMovie(id)) {
            throw notFound();
        }
        return ResponseEntity.noContent().build();
    }

    private static void requireAdmin(Account account) {
        if (!account.isAdmin()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "error.admin.required");
        }
    }

    private static ApiException notFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "error.movie.notFound");
    }
}
