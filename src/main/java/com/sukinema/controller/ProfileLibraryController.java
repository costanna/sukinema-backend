package com.sukinema.controller;

import com.sukinema.auth.AuthInterceptor;
import com.sukinema.model.Account;
import com.sukinema.model.Movie;
import com.sukinema.service.ProfileLibraryService;
import com.sukinema.service.ProfileLibraryService.Library;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profiles/{profileId}")
public class ProfileLibraryController {

    private final ProfileLibraryService libraryService;

    public ProfileLibraryController(ProfileLibraryService libraryService) {
        this.libraryService = libraryService;
    }

    @GetMapping("/library")
    public ResponseEntity<Library> getLibrary(
            @RequestAttribute(AuthInterceptor.ACCOUNT_ATTRIBUTE) Account account, @PathVariable Long profileId) {
        return ResponseEntity.ok(libraryService.getLibrary(account, profileId));
    }

    @PutMapping("/my-list/{movieId}")
    public ResponseEntity<Void> addToMyList(
            @RequestAttribute(AuthInterceptor.ACCOUNT_ATTRIBUTE) Account account,
            @PathVariable Long profileId, @PathVariable Long movieId) {
        libraryService.addToMyList(account, profileId, movieId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/my-list/{movieId}")
    public ResponseEntity<Void> removeFromMyList(
            @RequestAttribute(AuthInterceptor.ACCOUNT_ATTRIBUTE) Account account,
            @PathVariable Long profileId, @PathVariable Long movieId) {
        libraryService.removeFromMyList(account, profileId, movieId);
        return ResponseEntity.noContent().build();
    }

    // Devuelven el tráiler con su contador de likes actualizado
    @PutMapping("/likes/{movieId}")
    public ResponseEntity<Movie> like(
            @RequestAttribute(AuthInterceptor.ACCOUNT_ATTRIBUTE) Account account,
            @PathVariable Long profileId, @PathVariable Long movieId) {
        return ResponseEntity.ok(libraryService.like(account, profileId, movieId));
    }

    @DeleteMapping("/likes/{movieId}")
    public ResponseEntity<Movie> unlike(
            @RequestAttribute(AuthInterceptor.ACCOUNT_ATTRIBUTE) Account account,
            @PathVariable Long profileId, @PathVariable Long movieId) {
        return ResponseEntity.ok(libraryService.unlike(account, profileId, movieId));
    }
}
