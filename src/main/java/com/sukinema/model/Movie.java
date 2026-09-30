package com.sukinema.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Entity
@Table(name = "movies")
public class Movie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El título es obligatorio")
    private String title;

    @Column(length = 2000)
    private String overview;

    @NotBlank(message = "La URL del tráiler es obligatoria")
    private String trailerUrl;

    private String youtubeId;

    @Column(length = 1000)
    private String backdropUrl;

    @Column(length = 1000)
    private String posterUrl;

    private Integer releaseYear;

    private String matchScore;

    private String ageRating;

    private String duration;

    private String category;

    private String genres;

    @Column(name = "movie_cast")
    private String cast;

    private String director;

    private boolean featured = false;

    private boolean trending = false;

    private int likes = 0;

    private LocalDateTime createdAt;

    public Movie() {
    }

    public Movie(String title, String overview, String trailerUrl, String backdropUrl,
                 String posterUrl, Integer releaseYear, String matchScore, String ageRating,
                 String duration, String category, String genres, String cast, String director,
                 boolean featured, boolean trending) {
        this.title = title;
        this.overview = overview;
        this.trailerUrl = trailerUrl;
        this.youtubeId = extractYoutubeId(trailerUrl);
        this.backdropUrl = backdropUrl;
        this.posterUrl = posterUrl;
        this.releaseYear = releaseYear;
        this.matchScore = matchScore != null ? matchScore : "95% de coincidencia";
        this.ageRating = ageRating != null ? ageRating : "+16";
        this.duration = duration != null ? duration : "Tráiler 2m 15s";
        this.category = category;
        this.genres = genres;
        this.cast = cast;
        this.director = director;
        this.featured = featured;
        this.trending = trending;
    }

    @PrePersist
    @PreUpdate
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (trailerUrl != null && (youtubeId == null || youtubeId.isEmpty())) {
            this.youtubeId = extractYoutubeId(trailerUrl);
        }
        if (matchScore == null) {
            this.matchScore = "95% de coincidencia";
        }
        if (ageRating == null) {
            this.ageRating = "+16";
        }
    }

    public static String extractYoutubeId(String url) {
        if (url == null || url.trim().isEmpty()) {
            return "";
        }
        url = url.trim();
        if (url.matches("^[a-zA-Z0-9_-]{11}$")) {
            return url;
        }

        Pattern pattern = Pattern.compile(
            "(?:https?://)?(?:www\\.)?(?:youtube\\.com/(?:watch\\?v=|embed/|v/|shorts/)|youtu\\.be/)([a-zA-Z0-9_-]{11})"
        );
        Matcher matcher = pattern.matcher(url);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return url;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getOverview() {
        return overview;
    }

    public void setOverview(String overview) {
        this.overview = overview;
    }

    public String getTrailerUrl() {
        return trailerUrl;
    }

    public void setTrailerUrl(String trailerUrl) {
        this.trailerUrl = trailerUrl;
        this.youtubeId = extractYoutubeId(trailerUrl);
    }

    public String getYoutubeId() {
        return youtubeId;
    }

    public void setYoutubeId(String youtubeId) {
        this.youtubeId = youtubeId;
    }

    public String getBackdropUrl() {
        return backdropUrl;
    }

    public void setBackdropUrl(String backdropUrl) {
        this.backdropUrl = backdropUrl;
    }

    public String getPosterUrl() {
        return posterUrl;
    }

    public void setPosterUrl(String posterUrl) {
        this.posterUrl = posterUrl;
    }

    public Integer getReleaseYear() {
        return releaseYear;
    }

    public void setReleaseYear(Integer releaseYear) {
        this.releaseYear = releaseYear;
    }

    public String getMatchScore() {
        return matchScore;
    }

    public void setMatchScore(String matchScore) {
        this.matchScore = matchScore;
    }

    public String getAgeRating() {
        return ageRating;
    }

    public void setAgeRating(String ageRating) {
        this.ageRating = ageRating;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getGenres() {
        return genres;
    }

    public void setGenres(String genres) {
        this.genres = genres;
    }

    public String getCast() {
        return cast;
    }

    public void setCast(String cast) {
        this.cast = cast;
    }

    public String getDirector() {
        return director;
    }

    public void setDirector(String director) {
        this.director = director;
    }

    public boolean isFeatured() {
        return featured;
    }

    public void setFeatured(boolean featured) {
        this.featured = featured;
    }

    public boolean isTrending() {
        return trending;
    }

    public void setTrending(boolean trending) {
        this.trending = trending;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public int getLikes() {
        return likes;
    }

    public void setLikes(int likes) {
        this.likes = likes;
    }
}
