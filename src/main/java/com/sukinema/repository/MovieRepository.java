package com.sukinema.repository;

import com.sukinema.model.Movie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MovieRepository extends JpaRepository<Movie, Long> {

    List<Movie> findByFeaturedTrue();

    List<Movie> findByTrendingTrue();

    List<Movie> findByCategoryIgnoreCase(String category);

    // :query llega con % y _ escapados (ver MovieService.searchMovies)
    @Query("SELECT m FROM Movie m WHERE " +
           "LOWER(m.title) LIKE LOWER(CONCAT('%', :query, '%')) ESCAPE '\\' OR " +
           "LOWER(m.genres) LIKE LOWER(CONCAT('%', :query, '%')) ESCAPE '\\' OR " +
           "LOWER(m.cast) LIKE LOWER(CONCAT('%', :query, '%')) ESCAPE '\\' OR " +
           "LOWER(m.director) LIKE LOWER(CONCAT('%', :query, '%')) ESCAPE '\\' OR " +
           "LOWER(m.overview) LIKE LOWER(CONCAT('%', :query, '%')) ESCAPE '\\'")
    List<Movie> searchMovies(@Param("query") String query);

    // Solo puede haber un tráiler destacado: al marcar uno, se desmarca el resto
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Movie m SET m.featured = false WHERE m.featured = true AND m.id <> :id")
    int clearFeaturedExcept(@Param("id") Long id);

    // Incremento en la propia base de datos: dos likes simultáneos no se pisan
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Movie m SET m.likes = m.likes + 1 WHERE m.id = :id")
    int incrementLikes(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Movie m SET m.likes = m.likes - 1 WHERE m.id = :id AND m.likes > 0")
    int decrementLikes(@Param("id") Long id);
}
