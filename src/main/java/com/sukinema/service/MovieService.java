package com.sukinema.service;

import com.sukinema.model.Movie;
import com.sukinema.repository.MovieRepository;
import com.sukinema.repository.UserProfileRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class MovieService {

    static final String TRENDING_ROW = "Tendencias de Hoy";

    private final MovieRepository movieRepository;
    private final UserProfileRepository userProfileRepository;

    public MovieService(MovieRepository movieRepository, UserProfileRepository userProfileRepository) {
        this.movieRepository = movieRepository;
        this.userProfileRepository = userProfileRepository;
    }

    public List<Movie> getAllMovies() {
        return movieRepository.findAll();
    }

    public Optional<Movie> getMovieById(Long id) {
        return movieRepository.findById(id);
    }

    public Optional<Movie> getFeaturedMovie() {
        return movieRepository.findByFeaturedTrue().stream().findFirst()
                .or(() -> movieRepository.findAll(Sort.by("id")).stream().findFirst());
    }

    public List<Movie> getTrendingMovies() {
        return movieRepository.findByTrendingTrue();
    }

    public List<Movie> getMoviesByCategory(String category) {
        return movieRepository.findByCategoryIgnoreCase(category);
    }

    /**
     * Fila de tendencias y una fila por categoría, en el orden en que aparecen. Las categorías que solo
     * se diferencian en mayúsculas van juntas, con el nombre de la primera.
     */
    public Map<String, List<Movie>> getMoviesGroupedByCategory() {
        List<Movie> movies = movieRepository.findAll(Sort.by("id"));
        Map<String, List<Movie>> grouped = new LinkedHashMap<>();
        Map<String, String> rowNames = new HashMap<>();

        List<Movie> trending = new ArrayList<>(movies.stream().filter(Movie::isTrending).toList());
        if (!trending.isEmpty()) {
            grouped.put(TRENDING_ROW, trending);
            rowNames.put(TRENDING_ROW.toLowerCase(Locale.ROOT), TRENDING_ROW);
        }
        for (Movie movie : movies) {
            String category = movie.getCategory() == null ? "" : movie.getCategory().trim();
            if (category.isEmpty()) {
                continue;
            }
            String row = rowNames.computeIfAbsent(category.toLowerCase(Locale.ROOT), key -> category);
            List<Movie> list = grouped.computeIfAbsent(row, key -> new ArrayList<>());
            if (!list.contains(movie)) {
                list.add(movie);
            }
        }
        return grouped;
    }

    public List<Movie> searchMovies(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getAllMovies();
        }
        // % y _ son comodines de LIKE: se escapan para buscarlos como texto
        String escaped = query.trim()
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return movieRepository.searchMovies(escaped);
    }

    @Transactional
    public Movie createMovie(Movie movie) {
        // Siempre es un alta: nunca se sobrescribe un tráiler existente ni se heredan likes
        movie.setId(null);
        movie.setLikes(0);
        movie.setCreatedAt(null);
        Movie saved = movieRepository.save(movie);
        keepSingleFeatured(saved);
        return saved;
    }

    @Transactional
    public boolean deleteMovie(Long id) {
        if (!movieRepository.existsById(id)) {
            return false;
        }
        userProfileRepository.removeMovieFromAllLists(id);
        userProfileRepository.removeMovieFromAllLikes(id);
        movieRepository.deleteById(id);
        return true;
    }

    @Transactional
    public Optional<Movie> updateMovie(Long id, Movie updated) {
        return movieRepository.findById(id).map(existing -> {
            existing.setTitle(updated.getTitle());
            existing.setOverview(updated.getOverview());
            existing.setTrailerUrl(updated.getTrailerUrl());
            existing.setBackdropUrl(updated.getBackdropUrl());
            existing.setPosterUrl(updated.getPosterUrl());
            existing.setReleaseYear(updated.getReleaseYear());
            existing.setAgeRating(updated.getAgeRating());
            existing.setDuration(updated.getDuration());
            existing.setCategory(updated.getCategory());
            existing.setGenres(updated.getGenres());
            existing.setCast(updated.getCast());
            existing.setDirector(updated.getDirector());
            existing.setFeatured(updated.isFeatured());
            existing.setTrending(updated.isTrending());
            existing.setMatchScore(updated.getMatchScore());
            Movie saved = movieRepository.save(existing);
            keepSingleFeatured(saved);
            return saved;
        });
    }

    private void keepSingleFeatured(Movie saved) {
        if (saved.isFeatured()) {
            movieRepository.clearFeaturedExcept(saved.getId());
        }
    }
}
