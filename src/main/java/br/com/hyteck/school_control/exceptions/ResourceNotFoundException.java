package br.com.hyteck.school_control.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Custom runtime exception indicating that a requested resource could not be found in the system.
 * This is typically used when an operation attempts to access or modify a resource
 * identified by an ID or other unique key that does not exist.
 * When thrown, it results in an HTTP 404 Not Found response by default,
 * due to the {@link ResponseStatus} annotation.
 */
@ResponseStatus(HttpStatus.NOT_FOUND) // HTTP 404 Not Found is appropriate as the server cannot find the requested resource.
public class ResourceNotFoundException extends RuntimeException{

    /**
     * Constructs a new ResourceNotFoundException with the specified detail message.
     *
     * @param message The detail message (which is saved for later retrieval by the {@link #getMessage()} method),
     *                typically indicating the type and identifier of the resource that was not found.
     */
    public ResourceNotFoundException(String message){
        super(message);
    }
}
