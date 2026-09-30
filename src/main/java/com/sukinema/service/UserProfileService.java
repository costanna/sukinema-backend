package com.sukinema.service;

import com.sukinema.exception.ApiException;
import com.sukinema.model.Account;
import com.sukinema.model.UserProfile;
import com.sukinema.repository.UserProfileRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/** Perfiles de una cuenta. Todas las operaciones se limitan a los perfiles de la cuenta indicada. */
@Service
public class UserProfileService {

    static final int MAX_PROFILES = 5;

    private final UserProfileRepository userProfileRepository;

    public UserProfileService(UserProfileRepository userProfileRepository) {
        this.userProfileRepository = userProfileRepository;
    }

    public List<UserProfile> getProfiles(Account account) {
        return userProfileRepository.findByAccountIdOrderByCreatedAtAscIdAsc(account.getId());
    }

    public Optional<UserProfile> getProfile(Account account, Long id) {
        return userProfileRepository.findByIdAndAccountId(id, account.getId());
    }

    @Transactional
    public UserProfile createProfile(Account account, UserProfile request) {
        if (userProfileRepository.countByAccountId(account.getId()) >= MAX_PROFILES) {
            throw new ApiException(HttpStatus.CONFLICT, "error.profile.max", MAX_PROFILES);
        }
        String name = request.getName().trim();
        if (userProfileRepository.existsByAccountIdAndNameIgnoreCase(account.getId(), name)) {
            throw new ApiException(HttpStatus.CONFLICT, "error.profile.duplicate");
        }
        // Se copia campo a campo: la petición no puede fijar el id ni la cuenta
        UserProfile profile = new UserProfile(name, request.getAvatar(), request.getColor(), request.isKid());
        profile.setAccount(account);
        return userProfileRepository.save(profile);
    }

    @Transactional
    public Optional<UserProfile> updateProfile(Account account, Long id, UserProfile updated) {
        return userProfileRepository.findByIdAndAccountId(id, account.getId()).map(existing -> {
            String name = updated.getName().trim();
            if (userProfileRepository.existsByAccountIdAndNameIgnoreCaseAndIdNot(account.getId(), name, id)) {
                throw new ApiException(HttpStatus.CONFLICT, "error.profile.duplicate");
            }
            existing.setName(name);
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

    @Transactional
    public boolean deleteProfile(Account account, Long id) {
        Optional<UserProfile> profile = userProfileRepository.findByIdAndAccountId(id, account.getId());
        if (profile.isEmpty()) {
            return false;
        }
        if (userProfileRepository.countByAccountId(account.getId()) <= 1) {
            throw new ApiException(HttpStatus.CONFLICT, "error.profile.last");
        }
        userProfileRepository.delete(profile.get());
        return true;
    }
}
