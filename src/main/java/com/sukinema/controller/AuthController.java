package com.sukinema.controller;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sukinema.auth.AuthInterceptor;
import com.sukinema.auth.TokenService;
import com.sukinema.model.Account;
import com.sukinema.service.AccountService;
import com.sukinema.service.AccountService.AccountWithRecoveryCode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    // BCrypt solo tiene en cuenta los primeros 72 bytes
    private static final String PASSWORD_RULE = "La contraseña debe tener entre 8 y 72 caracteres";

    public record RegisterRequest(
            @NotBlank(message = "El nombre es obligatorio")
            @Size(max = 40, message = "El nombre no puede superar los 40 caracteres")
            String name,
            @NotBlank(message = "El correo es obligatorio")
            @Email(message = "El correo no tiene un formato válido")
            @Size(max = 120, message = "El correo no puede superar los 120 caracteres")
            String email,
            @NotBlank(message = "La contraseña es obligatoria")
            @Size(min = 8, max = 72, message = PASSWORD_RULE)
            String password) {

        // Un correo pegado con espacios alrededor sigue siendo válido
        public RegisterRequest {
            name = name == null ? null : name.trim();
            email = email == null ? null : email.trim();
        }
    }

    public record LoginRequest(
            @NotBlank(message = "El correo es obligatorio") String email,
            @NotBlank(message = "La contraseña es obligatoria") String password) {
    }

    public record RecoverRequest(
            @NotBlank(message = "El correo es obligatorio") String email,
            @NotBlank(message = "El código de recuperación es obligatorio") String recoveryCode,
            @NotBlank(message = "La contraseña nueva es obligatoria")
            @Size(min = 8, max = 72, message = PASSWORD_RULE)
            String newPassword) {
    }

    public record ChangePasswordRequest(
            @NotBlank(message = "La contraseña actual es obligatoria") String currentPassword,
            @NotBlank(message = "La contraseña nueva es obligatoria")
            @Size(min = 8, max = 72, message = PASSWORD_RULE)
            String newPassword) {
    }

    public record PasswordRequest(@NotBlank(message = "La contraseña es obligatoria") String password) {
    }

    public record AccountView(Long id, String name, String email, String role) {
        static AccountView of(Account account) {
            return new AccountView(account.getId(), account.getName(), account.getEmail(), account.getRole().name());
        }
    }

    // recoveryCode solo viaja cuando se acaba de generar uno (registro y recuperación)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record AuthResponse(String token, AccountView account, String recoveryCode) {
    }

    private final AccountService accountService;
    private final TokenService tokenService;

    public AuthController(AccountService accountService, TokenService tokenService) {
        this.accountService = accountService;
        this.tokenService = tokenService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AccountWithRecoveryCode created = accountService.register(request.name(), request.email(), request.password());
        return ResponseEntity.status(HttpStatus.CREATED).body(session(created.account(), created.recoveryCode()));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(session(accountService.authenticate(request.email(), request.password()), null));
    }

    @PostMapping("/recover")
    public ResponseEntity<AuthResponse> recover(@Valid @RequestBody RecoverRequest request) {
        AccountWithRecoveryCode recovered = accountService.recover(request.email(), request.recoveryCode(), request.newPassword());
        return ResponseEntity.ok(session(recovered.account(), recovered.recoveryCode()));
    }

    @GetMapping("/me")
    public ResponseEntity<AccountView> me(@RequestAttribute(AuthInterceptor.ACCOUNT_ATTRIBUTE) Account account) {
        return ResponseEntity.ok(AccountView.of(account));
    }

    // Cambiar la contraseña cierra las demás sesiones; la respuesta trae un token nuevo para seguir en esta
    @PostMapping("/password")
    public ResponseEntity<AuthResponse> changePassword(
            @RequestAttribute(AuthInterceptor.ACCOUNT_ATTRIBUTE) Account account,
            @Valid @RequestBody ChangePasswordRequest request) {
        Account updated = accountService.changePassword(account, request.currentPassword(), request.newPassword());
        return ResponseEntity.ok(session(updated, null));
    }

    @PostMapping("/recovery-code")
    public ResponseEntity<Map<String, String>> regenerateRecoveryCode(
            @RequestAttribute(AuthInterceptor.ACCOUNT_ATTRIBUTE) Account account,
            @Valid @RequestBody PasswordRequest request) {
        return ResponseEntity.ok(Map.of("recoveryCode", accountService.regenerateRecoveryCode(account, request.password())));
    }

    private AuthResponse session(Account account, String recoveryCode) {
        return new AuthResponse(tokenService.issue(account), AccountView.of(account), recoveryCode);
    }
}
