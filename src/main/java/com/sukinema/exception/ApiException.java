package com.sukinema.exception;

import org.springframework.http.HttpStatus;

/** Error de negocio con el código HTTP y el mensaje que debe ver quien usa la app. */
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
