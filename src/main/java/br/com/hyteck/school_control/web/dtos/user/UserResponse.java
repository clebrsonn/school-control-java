package br.com.hyteck.school_control.web.dtos.user;

import br.com.hyteck.school_control.models.auth.Role;
import br.com.hyteck.school_control.models.auth.User; // Importar entidade User

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Data Transfer Object (DTO) for returning user data to clients.
 * This DTO provides a representation of a user's details, excluding sensitive
 * information like the password.
 *
 * @param id                    The unique identifier of the user.
 * @param username              The username of the user.
 * @param email                 The email address of the user.
 * @param roles                 A set of role names assigned to the user (e.g., "ROLE_USER", "ROLE_ADMIN").
 * @param enabled               Indicates whether the user's account is enabled.
 * @param accountNonLocked      Indicates whether the user's account is not locked.
 * @param accountNonExpired     Indicates whether the user's account has not expired.
 * @param credentialsNonExpired Indicates whether the user's credentials (password) have not expired.
 * @param createdAt             The timestamp when the user account was created.
 * @param updatedAt             The timestamp when the user account was last updated.
 */
public record UserResponse(
        String id,
        String username,
        String email,
        Set<String> roles,
        boolean enabled,
        boolean accountNonLocked,
        boolean accountNonExpired,
        boolean credentialsNonExpired,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    /**
     * Static factory method to create a {@link UserResponse} from a {@link User} entity.
     * This method performs the mapping from the domain model to the DTO.
     *
     * @param user The {@link User} entity to convert.
     * @return A {@link UserResponse} populated with data from the User entity, or {@code null} if the input user is {@code null}.
     */
    public static UserResponse from(User user) {
        // Return null if the source User entity is null to prevent NullPointerExceptions.
        if (user == null) {
            return null;
        }
        // Create and return a new UserResponse, mapping fields from the User entity.
        return new UserResponse(
                user.getId(), // User's unique ID.
                user.getUsername(), // Username.
                user.getEmail(), // Email address.
                user.getRoles().stream() // Stream over the Set<Role>
                        .map(Role::getName) // Map each Role object to its name (String).
                        .collect(Collectors.toSet()), // Collect the role names into a Set<String>.
                user.isEnabled(), // Account enabled status.
                user.isAccountNonLocked(), // Account locked status.
                user.isAccountNonExpired(), // Account expiration status.
                user.isCredentialsNonExpired(), // Credentials expiration status.
                user.getCreatedAt(), // Timestamp of account creation.
                user.getUpdatedAt() // Timestamp of last account update.
        );
    }
}