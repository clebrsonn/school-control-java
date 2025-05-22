package br.com.hyteck.school_control.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Custom runtime exception indicating that a requested deletion operation cannot be performed.
 * This is typically due to business rules, data integrity constraints (e.g., existing related records),
 * or security restrictions.
 * When thrown, it results in an HTTP 409 Conflict response by default,
 * due to the {@link ResponseStatus} annotation.
 */
@ResponseStatus(HttpStatus.CONFLICT) // HTTP 409 Conflict is appropriate as the request cannot be processed due to the current state of the resource.
public class DeletionNotAllowedException extends RuntimeException {

    /**
     * Constructs a new DeletionNotAllowedException with the specified detail message.
     *
     * @param message The detail message (which is saved for later retrieval by the {@link #getMessage()} method),
     *                explaining why the deletion is not allowed.
     */
    public DeletionNotAllowedException(String message) {
        super(message);
    }
}
