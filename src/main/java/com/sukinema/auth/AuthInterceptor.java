package com.sukinema.auth;

import com.sukinema.model.Account;
import com.sukinema.repository.AccountRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.Optional;

/**
 * Exige un token de sesión válido en las rutas protegidas y deja la cuenta
 * autenticada en el atributo de petición {@link #ACCOUNT_ATTRIBUTE}.
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    public static final String ACCOUNT_ATTRIBUTE = "account";
    private static final String BEARER_PREFIX = "Bearer ";

    private final TokenService tokenService;
    private final AccountRepository accountRepository;

    public AuthInterceptor(TokenService tokenService, AccountRepository accountRepository) {
        this.tokenService = tokenService;
        this.accountRepository = accountRepository;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        // Las comprobaciones previas de CORS del navegador no llevan token
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        Optional<Account> account = header != null && header.startsWith(BEARER_PREFIX)
                ? tokenService.verify(header.substring(BEARER_PREFIX.length()).trim())
                        // Se consulta la cuenta en cada petición: una cuenta borrada deja de valer al momento,
                        // igual que las sesiones anteriores a un cambio de contraseña
                        .flatMap(token -> accountRepository.findById(token.accountId())
                                .filter(found -> found.getSessionsValidFrom() == null
                                        || !token.issuedAt().isBefore(found.getSessionsValidFrom())))
                : Optional.empty();

        if (account.isEmpty()) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"message\":\"Inicia sesión para continuar.\"}");
            return false;
        }

        request.setAttribute(ACCOUNT_ATTRIBUTE, account.get());
        return true;
    }
}
