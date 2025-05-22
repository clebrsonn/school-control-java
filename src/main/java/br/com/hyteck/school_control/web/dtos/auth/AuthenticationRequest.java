package br.com.hyteck.school_control.web.dtos.auth;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object (DTO) representing the request body for user authentication.
 * It contains the credentials (login and password) required to authenticate a user.
 * Lombok's {@link Data} annotation automatically generates getters, setters,
 * `toString`, `equals`, and `hashCode` methods.
 * {@link AllArgsConstructor} and {@link NoArgsConstructor} generate respective constructors.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthenticationRequest {

    /**
     * The user's login identifier, typically their username or email address.
     * It must not be null and its size must not exceed 255 characters.
     */
    @NotNull(message = "Login cannot be null.")
    @Size(max = 255, message = "Login must not exceed 255 characters.")
    private String login;

    /**
     * The user's password.
     * It must not be null and its size must not exceed 255 characters.
     * This password should be sent over HTTPS and will be compared against a hashed version stored in the database.
     */
    @NotNull(message = "Password cannot be null.")
    @Size(max = 255, message = "Password must not exceed 255 characters.")
    private String password;

}
