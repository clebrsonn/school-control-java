package br.com.hyteck.school_control.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Custom runtime exception to represent business rule violations or invalid operations.
 * When thrown, it results in an HTTP 400 Bad Request response by default,
 * due to the {@link ResponseStatus} annotation.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST) // Maps this exception to HTTP 400 Bad Request
public class BusinessException extends RuntimeException {

    /**
     * Constructs a new BusinessException with the specified detail message.
     *
     * @param message The detail message (which is saved for later retrieval by the {@link #getMessage()} method).
     */
    public BusinessException(String message) {
        super(message);
    }
}
