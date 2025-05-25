package br.com.hyteck.school_control.web.dtos.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Data Transfer Object (DTO) for creating or updating a user.
 * When creating a user, the password field is typically required.
 * For updates, the password field might be optional, allowing other user details
 * to be changed without modifying the password.
 *
 * @param username The desired username for the account. Must be unique and between 3 and 50 characters.
 * @param password The user's password. For new users, this is required and should be between 6 and 100 characters.
 *                 For updates, this may be optional. Password complexity rules are typically enforced by the service layer.
 * @param email    The user's email address. Must be a valid email format and not exceed 100 characters.
 *                 It's often used for communication and account recovery.
 */
public record UserRequest(
        @NotBlank(message = "Username cannot be blank.")
        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters.")
        String username,

        // Password might not be @NotBlank as it's optional for updates.
        // Service layer should handle logic for requiring it on creation.
        @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters if provided.")
        String password,

        @NotBlank(message = "Email cannot be blank.")
        @Email(message = "Email should be valid.")
        @Size(max = 100, message = "Email must not exceed 100 characters.")
        String email) {
}