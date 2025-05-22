package br.com.hyteck.school_control.web.dtos.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Data Transfer Object (DTO) representing the request to change a user's password.
 * This record encapsulates the username, the user's current password, and the new desired password.
 *
 * @param username        The username of the user requesting the password change. Must not be blank.
 * @param currentPassword The user's current password. Must not be blank and must be between 6 and 100 characters.
 * @param newPassword     The new password the user wants to set. Should meet defined password complexity rules (validated by the service layer).
 */
public record PasswordChangeRequest(
        @NotBlank(message = "Username cannot be blank.")
        String username,

        @NotBlank(message = "Current password cannot be blank.")
        @Size(min = 6, max = 100, message = "Current password must be between 6 and 100 characters.")
        String currentPassword,

        // It's generally a good idea to also validate the new password here,
        // for example, with @NotBlank and @Size, though more complex rules
        // might be handled in the service layer.
        @NotBlank(message = "New password cannot be blank.")
        @Size(min = 6, max = 100, message = "New password must be between 6 and 100 characters.")
        String newPassword
){
}
