package com.sukinema.service;

import com.sukinema.model.UserProfile;
import com.sukinema.repository.UserProfileRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserProfileService {

    private final UserProfileRepository userProfileRepository;

    public UserProfileService(UserProfileRepository userProfileRepository) {
        this.userProfileRepository = userProfileRepository;
    }

    public List<UserProfile> getAllProfiles() {
        return userProfileRepository.findAllByOrderByCreatedAtAsc();
    }

    public Optional<UserProfile> getProfileById(Long id) {
        return userProfileRepository.findById(id);
    }

    public UserProfile createProfile(UserProfile profile) {
        return userProfileRepository.save(profile);
    }

    public Optional<UserProfile> updateProfile(Long id, UserProfile updated) {
        return userProfileRepository.findById(id).map(existing -> {
            existing.setName(updated.getName());
            // Avatar y color se conservan si no vienen en la petición
            if (updated.getAvatar() != null && !updated.getAvatar().trim().isEmpty()) {
                existing.setAvatar(updated.getAvatar());
            }
            if (updated.getColor() != null && !updated.getColor().trim().isEmpty()) {
                existing.setColor(updated.getColor());
            }
            existing.setKid(updated.isKid());
            return userProfileRepository.save(existing);
        });
    }

    public boolean deleteProfile(Long id) {
        if (userProfileRepository.existsById(id)) {
            userProfileRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public long count() {
        return userProfileRepository.count();
    }
}
