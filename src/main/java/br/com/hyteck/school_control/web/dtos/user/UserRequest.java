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

        // Senha não é @NotBlank aqui, pois pode ser opcional na atualização
        @Size(min = 6, max = 100) // Validar tamanho se fornecida
        String password, // Será obrigatório apenas na lógica de criação

        @NotBlank
        @Email
        @Size(max = 100)
        String email) {
}