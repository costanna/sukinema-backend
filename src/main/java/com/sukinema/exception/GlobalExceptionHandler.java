package com.sukinema.exception;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Todas las respuestas de error llevan la forma {"message": "..."} para que el frontend pueda
 * mostrarlas, en el idioma que pide la petición (cabecera Accept-Language).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private final MessageSource messageSource;

    public GlobalExceptionHandler(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, String>> handleApiException(ApiException e) {
        return body(e.getStatus(), e.getMessageKey(), e.getArgs());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException e) {
        // Los mensajes de validación ya llegan traducidos: las anotaciones apuntan a claves de messages*.properties
        return e.getBindingResult().getAllErrors().stream()
                .map(error -> error.getDefaultMessage())
                .filter(text -> text != null && !text.isBlank())
                .findFirst()
                .map(message -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", message)))
                .orElseGet(() -> body(HttpStatus.BAD_REQUEST, "error.request.invalid"));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleUnreadable(HttpMessageNotReadableException e) {
        return body(HttpStatus.BAD_REQUEST, "error.request.unreadable");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleConflict(DataIntegrityViolationException e) {
        return body(HttpStatus.CONFLICT, "error.request.conflict");
    }

    private ResponseEntity<Map<String, String>> body(HttpStatus status, String messageKey, Object... args) {
        String message = messageSource.getMessage(messageKey, args, messageKey, LocaleContextHolder.getLocale());
        return ResponseEntity.status(status).body(Map.of("message", message));
    }
}
