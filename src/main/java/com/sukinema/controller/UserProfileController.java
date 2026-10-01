package com.sukinema.controller;

import com.sukinema.auth.AuthInterceptor;
import com.sukinema.exception.ApiException;
import com.sukinema.model.Account;
import com.sukinema.model.UserProfile;
import com.sukinema.service.UserProfileService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/profiles")
public class UserProfileController {

    private final UserProfileService userProfileService;

    public UserProfileController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @GetMapping
    public ResponseEntity<List<UserProfile>> getAllProfiles(
            @RequestAttribute(AuthInterceptor.ACCOUNT_ATTRIBUTE) Account account) {
        return ResponseEntity.ok(userProfileService.getProfiles(account));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserProfile> getProfileById(
            @RequestAttribute(AuthInterceptor.ACCOUNT_ATTRIBUTE) Account account, @PathVariable Long id) {
        return ResponseEntity.ok(userProfileService.getProfile(account, id).orElseThrow(UserProfileController::notFound));
    }

    @PostMapping
    public ResponseEntity<UserProfile> createProfile(
            @RequestAttribute(AuthInterceptor.ACCOUNT_ATTRIBUTE) Account account,
            @Valid @RequestBody UserProfile profile) {
        UserProfile created = userProfileService.createProfile(account, profile);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserProfile> updateProfile(
            @RequestAttribute(AuthInterceptor.ACCOUNT_ATTRIBUTE) Account account,
            @PathVariable Long id, @Valid @RequestBody UserProfile profile) {
        return ResponseEntity.ok(userProfileService.updateProfile(account, id, profile).orElseThrow(UserProfileController::notFound));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProfile(
            @RequestAttribute(AuthInterceptor.ACCOUNT_ATTRIBUTE) Account account, @PathVariable Long id) {
        if (!userProfileService.deleteProfile(account, id)) {
            throw notFound();
        }
        return ResponseEntity.noContent().build();
    }

    private static ApiException notFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "error.profile.notFound");
    }
}
