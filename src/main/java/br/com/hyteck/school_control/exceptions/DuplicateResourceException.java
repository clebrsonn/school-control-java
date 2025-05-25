package br.com.hyteck.school_control.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Custom runtime exception indicating that an attempt to create a resource failed
 * because a resource with the same unique identifier (e.g., email, CPF, name) already exists.
 * When thrown, it results in an HTTP 409 Conflict response by default,
 * due to the {@link ResponseStatus} annotation.
 */
@ResponseStatus(HttpStatus.CONFLICT) // HTTP 409 Conflict is appropriate as the request conflicts with the current state of the resource.
public class DuplicateResourceException extends RuntimeException {

    /**
     * Constructs a new DuplicateResourceException with the specified detail message.
     *
     * @param message The detail message (which is saved for later retrieval by the {@link #getMessage()} method),
     *                typically indicating which resource or field caused the duplication.
     */
    public DuplicateResourceException(String message) {
        super(message);
    }
}
