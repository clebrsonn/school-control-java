package br.com.hyteck.school_control.web.dtos.student;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.br.CPF; // Asegúrate de tener esta dependencia si la usas

/**
 * DTO para la solicitud de creación de un estudiante.
 */
public record StudentRequest(
        @NotBlank
        @Size(max = 255)
        String name,

        @NotBlank
        @Email
        @Size(max = 100)
        String email,

        @CPF
        String cpf,

        @NotNull
        String responsibleId
) {
}