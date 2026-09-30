package com.sukinema.service;

import com.sukinema.auth.LoginAttemptLimiter;
import com.sukinema.exception.ApiException;
import com.sukinema.model.Account;
import com.sukinema.model.Role;
import com.sukinema.model.UserProfile;
import com.sukinema.repository.AccountRepository;
import com.sukinema.repository.UserProfileRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Optional;

@Service
public class AccountService {

    /** Una cuenta junto al código de recuperación recién generado, que solo se puede mostrar en ese momento. */
    public record AccountWithRecoveryCode(Account account, String recoveryCode) {
    }

    private static final int PROFILE_NAME_MAX = 20;
    // Sin 0/O ni 1/I, que se confunden al copiar el código a mano
    private static final String RECOVERY_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int RECOVERY_CODE_LENGTH = 12;
    private static final String TOO_MANY_ATTEMPTS = "error.account.tooManyAttempts";

    private final AccountRepository accountRepository;
    private final UserProfileRepository userProfileRepository;
    private final LoginAttemptLimiter loginAttemptLimiter;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final SecureRandom random = new SecureRandom();
    // Con un correo desconocido se comprueba igualmente una contraseña, para que la respuesta tarde lo mismo
    private final String dummyHash = passwordEncoder.encode("sukinema");
    private final String adminEmail;

    public AccountService(AccountRepository accountRepository,
                          UserProfileRepository userProfileRepository,
                          LoginAttemptLimiter loginAttemptLimiter,
                          @Value("${app.admin-email:}") String adminEmail) {
        this.accountRepository = accountRepository;
        this.userProfileRepository = userProfileRepository;
        this.loginAttemptLimiter = loginAttemptLimiter;
        this.adminEmail = normalizeEmail(adminEmail);
    }

    @Transactional
    public AccountWithRecoveryCode register(String name, String email, String password) {
        String normalizedEmail = normalizeEmail(email);
        if (accountRepository.existsByEmail(normalizedEmail)) {
            throw new ApiException(HttpStatus.CONFLICT, "error.account.emailTaken");
        }

        // Administra el catálogo la cuenta de ADMIN_EMAIL o, si no se ha definido, la primera que se registra
        boolean isAdmin = adminEmail.isEmpty()
                ? accountRepository.count() == 0
                : adminEmail.equals(normalizedEmail);

        Account account = new Account(
                normalizedEmail, name.trim(), passwordEncoder.encode(password), isAdmin ? Role.ADMIN : Role.USER);
        String recoveryCode = assignNewRecoveryCode(account);
        account = accountRepository.save(account);

        if (isAdmin) {
            userProfileRepository.claimOrphans(account);
        }
        if (userProfileRepository.countByAccountId(account.getId()) == 0) {
            String profileName = account.getName().length() > PROFILE_NAME_MAX
                    ? account.getName().substring(0, PROFILE_NAME_MAX).trim()
                    : account.getName();
            UserProfile profile = new UserProfile(profileName, null, null, false);
            profile.setAccount(account);
            userProfileRepository.save(profile);
        }
        return new AccountWithRecoveryCode(account, recoveryCode);
    }

    public Account authenticate(String email, String password) {
        String normalizedEmail = normalizeEmail(email);
        if (loginAttemptLimiter.isBlocked(normalizedEmail)) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, TOO_MANY_ATTEMPTS);
        }

        Optional<Account> account = accountRepository.findByEmail(normalizedEmail);
        boolean matches = passwordEncoder.matches(password, account.map(Account::getPasswordHash).orElse(dummyHash));
        if (account.isEmpty() || !matches) {
            loginAttemptLimiter.recordFailure(normalizedEmail);
            // Mismo mensaje en los dos casos: no se revela qué correos tienen cuenta
            throw new ApiException(HttpStatus.UNAUTHORIZED, "error.account.badCredentials");
        }

        loginAttemptLimiter.reset(normalizedEmail);
        return account.get();
    }

    /** Pone una contraseña nueva a quien demuestra tener el código de recuperación de la cuenta. */
    @Transactional
    public AccountWithRecoveryCode recover(String email, String recoveryCode, String newPassword) {
        String normalizedEmail = normalizeEmail(email);
        // Contador aparte del de inicio de sesión: adivinar el código también se frena
        String limiterKey = "recover:" + normalizedEmail;
        if (loginAttemptLimiter.isBlocked(limiterKey)) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, TOO_MANY_ATTEMPTS);
        }

        Optional<Account> found = accountRepository.findByEmail(normalizedEmail);
        String storedHash = found.map(Account::getRecoveryCodeHash).orElse(null);
        boolean matches = passwordEncoder.matches(normalizeRecoveryCode(recoveryCode), storedHash != null ? storedHash : dummyHash);
        if (found.isEmpty() || storedHash == null || !matches) {
            loginAttemptLimiter.recordFailure(limiterKey);
            throw new ApiException(HttpStatus.UNAUTHORIZED, "error.account.badRecovery");
        }

        Account account = found.get();
        setPassword(account, newPassword);
        // El código es de un solo uso: se sustituye por uno nuevo
        String newRecoveryCode = assignNewRecoveryCode(account);
        accountRepository.save(account);
        loginAttemptLimiter.reset(limiterKey);
        loginAttemptLimiter.reset(normalizedEmail);
        return new AccountWithRecoveryCode(account, newRecoveryCode);
    }

    @Transactional
    public Account changePassword(Account account, String currentPassword, String newPassword) {
        requirePassword(account, currentPassword);
        setPassword(account, newPassword);
        return accountRepository.save(account);
    }

    @Transactional
    public String regenerateRecoveryCode(Account account, String password) {
        requirePassword(account, password);
        String recoveryCode = assignNewRecoveryCode(account);
        accountRepository.save(account);
        return recoveryCode;
    }

    private void requirePassword(Account account, String password) {
        if (!passwordEncoder.matches(password, account.getPasswordHash())) {
            // 403 y no 401: la sesión es válida, lo que falla es esta comprobación
            throw new ApiException(HttpStatus.FORBIDDEN, "error.account.wrongPassword");
        }
    }

    private void setPassword(Account account, String newPassword) {
        account.setPasswordHash(passwordEncoder.encode(newPassword));
        // Cierra las sesiones abiertas en otros dispositivos; los tokens llevan precisión de segundos
        account.setSessionsValidFrom(Instant.now().truncatedTo(ChronoUnit.SECONDS));
    }

    private String assignNewRecoveryCode(Account account) {
        StringBuilder code = new StringBuilder(RECOVERY_CODE_LENGTH);
        for (int i = 0; i < RECOVERY_CODE_LENGTH; i++) {
            code.append(RECOVERY_ALPHABET.charAt(random.nextInt(RECOVERY_ALPHABET.length())));
        }
        account.setRecoveryCodeHash(passwordEncoder.encode(code.toString()));
        // Se muestra en grupos de cuatro para que sea más fácil de copiar: ABCD-EFGH-JKLM
        return code.substring(0, 4) + "-" + code.substring(4, 8) + "-" + code.substring(8, 12);
    }

    private static String normalizeRecoveryCode(String recoveryCode) {
        return recoveryCode == null ? "" : recoveryCode.replaceAll("[\\s-]", "").toUpperCase(Locale.ROOT);
    }

    private static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
