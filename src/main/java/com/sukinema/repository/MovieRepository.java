package com.sukinema.repository;

import com.sukinema.model.Movie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MovieRepository extends JpaRepository<Movie, Long> {

    List<Movie> findByFeaturedTrue();

    List<Movie> findByTrendingTrue();

    List<Movie> findByCategoryIgnoreCase(String category);

    @Query("SELECT DISTINCT m.category FROM Movie m WHERE m.category IS NOT NULL")
    List<String> findDistinctCategories();

    @Query("SELECT m FROM Movie m WHERE " +
           "LOWER(m.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(m.genres) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(m.cast) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(m.overview) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Movie> searchMovies(@Param("query") String query);
}
