package com.sukinema.exception;

import org.springframework.http.HttpStatus;

/**
 * Error de negocio con el código HTTP y la clave (en messages*.properties) del mensaje
 * que debe ver quien usa la app. El texto se resuelve al responder, en el idioma de la petición.
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final transient Object[] args;

    public ApiException(HttpStatus status, String messageKey, Object... args) {
        super(messageKey);
        this.status = status;
        this.args = args;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getMessageKey() {
        return getMessage();
    }

    public Object[] getArgs() {
        return args;
    }
}
