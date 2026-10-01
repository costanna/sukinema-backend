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

    private static final String PASSWORD_RULE = "{validation.account.password.size}";

    public record RegisterRequest(
            @NotBlank(message = "{validation.account.name.required}")
            @Size(max = 40, message = "{validation.account.name.size}")
            String name,
            @NotBlank(message = "{validation.account.email.required}")
            @Email(message = "{validation.account.email.format}")
            @Size(max = 120, message = "{validation.account.email.size}")
            String email,
            @NotBlank(message = "{validation.account.password.required}")
            @Size(min = 8, max = 72, message = PASSWORD_RULE)
            String password) {

        // Un correo pegado con espacios alrededor sigue siendo válido
        public RegisterRequest {
            name = name == null ? null : name.trim();
            email = email == null ? null : email.trim();
        }
    }

    public record LoginRequest(
            @NotBlank(message = "{validation.account.email.required}") String email,
            @NotBlank(message = "{validation.account.password.required}") String password) {
    }

    public record RecoverRequest(
            @NotBlank(message = "{validation.account.email.required}") String email,
            @NotBlank(message = "{validation.account.recoveryCode.required}") String recoveryCode,
            @NotBlank(message = "{validation.account.newPassword.required}")
            @Size(min = 8, max = 72, message = PASSWORD_RULE)
            String newPassword) {
    }

    public record ChangePasswordRequest(
            @NotBlank(message = "{validation.account.currentPassword.required}") String currentPassword,
            @NotBlank(message = "{validation.account.newPassword.required}")
            @Size(min = 8, max = 72, message = PASSWORD_RULE)
            String newPassword) {
    }

    public record PasswordRequest(@NotBlank(message = "{validation.account.password.required}") String password) {
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
