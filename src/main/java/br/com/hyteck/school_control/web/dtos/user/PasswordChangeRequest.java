package br.com.hyteck.school_control.web.dtos.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordChangeRequest(
        @NotBlank
        String username,

        @NotBlank
        @Size(min = 6, max = 100)
        String currentPassword,

        String newPassword
){
}
