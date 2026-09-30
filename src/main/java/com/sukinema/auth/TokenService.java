package com.sukinema.auth;

import com.sukinema.model.Account;
import com.sukinema.model.AppSetting;
import com.sukinema.repository.AppSettingRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;

/** Emite y valida los tokens de sesión (JWT firmados con HMAC-SHA256). */
@Service
public class TokenService {

    /** Lo que se sabe de un token auténtico: de qué cuenta es y cuándo se emitió. */
    public record VerifiedToken(Long accountId, Instant issuedAt) {
    }

    static final String SECRET_SETTING = "jwt_secret";
    private static final Duration SESSION_DURATION = Duration.ofDays(7);

    private final SecretKey key;

    public TokenService(AppSettingRepository settings, @Value("${app.jwt.secret:}") String configuredSecret) {
        // Sin JWT_SECRET, la clave se genera una vez y se guarda en la base de datos:
        // así las sesiones siguen valiendo después de cada reinicio del servidor.
        String secret = configuredSecret == null || configuredSecret.isBlank()
                ? settings.findById(SECRET_SETTING)
                        .orElseGet(() -> settings.save(new AppSetting(SECRET_SETTING, randomSecret())))
                        .getSettingValue()
                : configuredSecret;
        this.key = Keys.hmacShaKeyFor(sha256(secret));
    }

    public String issue(Account account) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(account.getId()))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(SESSION_DURATION)))
                .signWith(key)
                .compact();
    }

    /** Vacío si el token no es auténtico o ha caducado. */
    public Optional<VerifiedToken> verify(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(new VerifiedToken(Long.parseLong(claims.getSubject()), claims.getIssuedAt().toInstant()));
        } catch (JwtException | IllegalArgumentException | NullPointerException e) {
            return Optional.empty();
        }
    }

    private static String randomSecret() {
        byte[] bytes = new byte[48];
        new SecureRandom().nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    private static byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
