package br.com.hyteck.school_control.web.dtos.error; // Exemplo de pacote

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Represents a standardized error response for API calls.
 * This record provides a consistent structure for returning error information to clients,
 * including a timestamp, HTTP status code, error type, a descriptive message, the request path,
 * and optional field-specific validation errors.
 *
 * @param timestamp   The exact time when the error occurred.
 * @param status      The HTTP status code (e.g., 400 for Bad Request, 404 for Not Found).
 * @param error       A short, human-readable representation of the error type (e.g., "Bad Request", "Validation Error").
 * @param message     A more detailed message explaining the error.
 * @param path        The URL path of the request that resulted in the error.
 * @param fieldErrors An optional map containing field-specific validation errors, where the key is the field name
 *                    and the value is the error message for that field. Used primarily for validation failures.
 */
public record ApiErrorResponse(
        Instant timestamp,
        Integer status,
        String error,
        String message,
        String path,
        Map<String, String> fieldErrors
) {
    /**
     * Constructs an {@code ApiErrorResponse} for general errors that do not involve field-specific validation issues.
     * The timestamp is automatically set to the current time, and fieldErrors is set to {@code null}.
     *
     * @param status  The HTTP status code.
     * @param error   The error type.
     * @param message A descriptive message for the error.
     * @param path    The request path that caused the error.
     */
    public ApiErrorResponse(Integer status, String error, String message, String path) {
        // Calls the canonical constructor, setting timestamp to now and fieldErrors to null.
        this(Instant.now(), status, error, message, path, null);
    }
}