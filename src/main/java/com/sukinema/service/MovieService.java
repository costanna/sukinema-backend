package com.sukinema.service;

import com.sukinema.model.Movie;
import com.sukinema.repository.MovieRepository;
import com.sukinema.repository.UserProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class MovieService {

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

    public Movie getFeaturedMovie() {
        List<Movie> featured = movieRepository.findByFeaturedTrue();
        if (!featured.isEmpty()) {
            return featured.get(0);
        }
        List<Movie> all = movieRepository.findAll();
        return all.isEmpty() ? null : all.get(0);
    }

    public List<Movie> getTrendingMovies() {
        return movieRepository.findByTrendingTrue();
    }

    public List<Movie> getMoviesByCategory(String category) {
        return movieRepository.findByCategoryIgnoreCase(category);
    }

    public Map<String, List<Movie>> getMoviesGroupedByCategory() {
        Map<String, List<Movie>> grouped = new LinkedHashMap<>();

        List<Movie> trending = movieRepository.findByTrendingTrue();
        if (!trending.isEmpty()) {
            grouped.put("Tendencias de Hoy", trending);
        }

        List<String> categories = movieRepository.findDistinctCategories();
        for (String cat : categories) {
            List<Movie> list = movieRepository.findByCategoryIgnoreCase(cat);
            if (!list.isEmpty()) {
                grouped.put(cat, list);
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
        if (movieRepository.existsById(id)) {
            userProfileRepository.removeMovieFromAllLists(id);
            userProfileRepository.removeMovieFromAllLikes(id);
            movieRepository.deleteById(id);
            return true;
        }
        return false;
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

    public long count() {
        return movieRepository.count();
    }

    private void keepSingleFeatured(Movie saved) {
        if (saved.isFeatured()) {
            movieRepository.clearFeaturedExcept(saved.getId());
        }
    }
}
