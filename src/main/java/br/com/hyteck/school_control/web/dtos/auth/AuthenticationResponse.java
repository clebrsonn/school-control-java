package br.com.hyteck.school_control.web.dtos.auth;

import br.com.hyteck.school_control.web.dtos.user.UserResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Data Transfer Object (DTO) representing the response body for a successful user authentication.
 * It contains the JWT (JSON Web Token) and details of the authenticated user.
 * Lombok's {@link Getter}, {@link Setter}, {@link AllArgsConstructor}, and {@link NoArgsConstructor}
 * annotations automatically generate the respective methods and constructors.
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class AuthenticationResponse {
    /**
     * The JWT (JSON Web Token) generated for the authenticated user.
     * This token should be included in the "Authorization" header of subsequent requests
     * to access protected resources.
     */
    private String token;

    /**
     * Detailed information about the authenticated user.
     * Contains user attributes such as ID, username, roles, etc.
     * See {@link UserResponse} for more details.
     */
    private UserResponse user;
}
