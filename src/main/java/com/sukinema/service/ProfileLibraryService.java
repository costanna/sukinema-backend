package com.sukinema.service;

import com.sukinema.exception.ApiException;
import com.sukinema.model.Account;
import com.sukinema.model.Movie;
import com.sukinema.model.UserProfile;
import com.sukinema.repository.MovieRepository;
import com.sukinema.repository.UserProfileRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.TreeSet;

/** "Mi Lista" y los likes de cada perfil. Solo se accede a los perfiles de la propia cuenta. */
@Service
public class ProfileLibraryService {

    public record Library(Set<Long> myList, Set<Long> likes) {
    }

    private final UserProfileRepository userProfileRepository;
    private final MovieRepository movieRepository;

    public ProfileLibraryService(UserProfileRepository userProfileRepository, MovieRepository movieRepository) {
        this.userProfileRepository = userProfileRepository;
        this.movieRepository = movieRepository;
    }

    @Transactional(readOnly = true)
    public Library getLibrary(Account account, Long profileId) {
        UserProfile profile = requireProfile(account, profileId);
        return new Library(new TreeSet<>(profile.getMyListMovieIds()), new TreeSet<>(profile.getLikedMovieIds()));
    }

    @Transactional
    public void addToMyList(Account account, Long profileId, Long movieId) {
        UserProfile profile = requireProfile(account, profileId);
        requireMovie(movieId);
        if (profile.getMyListMovieIds().add(movieId)) {
            userProfileRepository.save(profile);
        }
    }

    @Transactional
    public void removeFromMyList(Account account, Long profileId, Long movieId) {
        UserProfile profile = requireProfile(account, profileId);
        if (profile.getMyListMovieIds().remove(movieId)) {
            userProfileRepository.save(profile);
        }
    }

    /** Un like por perfil y tráiler: repetirlo no vuelve a sumar. */
    @Transactional
    public Movie like(Account account, Long profileId, Long movieId) {
        UserProfile profile = requireProfile(account, profileId);
        requireMovie(movieId);
        if (profile.getLikedMovieIds().add(movieId)) {
            userProfileRepository.save(profile);
            movieRepository.incrementLikes(movieId);
        }
        return requireMovie(movieId);
    }

    @Transactional
    public Movie unlike(Account account, Long profileId, Long movieId) {
        UserProfile profile = requireProfile(account, profileId);
        requireMovie(movieId);
        if (profile.getLikedMovieIds().remove(movieId)) {
            userProfileRepository.save(profile);
            movieRepository.decrementLikes(movieId);
        }
        return requireMovie(movieId);
    }

    private UserProfile requireProfile(Account account, Long profileId) {
        return userProfileRepository.findByIdAndAccountId(profileId, account.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "error.profile.notFound"));
    }

    private Movie requireMovie(Long movieId) {
        return movieRepository.findById(movieId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "error.movie.notFound"));
    }
}
