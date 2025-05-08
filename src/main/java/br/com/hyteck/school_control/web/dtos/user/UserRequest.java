package br.com.hyteck.school_control.web.dtos.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO para criar ou atualizar um usuário.
 * A senha só é obrigatória na criação.
 */
public record UserRequest(
        @NotBlank
        @Size(min = 3, max = 50)
        String username,

        @Size(min = 6, max = 100)
        String password,

        @NotBlank
        @Email
        @Size(max = 100)
        String email) {
}