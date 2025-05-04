package br.com.hyteck.school_control.web.dtos.responsible;

import br.com.hyteck.school_control.models.payments.Responsible;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.br.CPF;

/**
 * DTO para receber os dados de criação de um novo Responsável.
 * Inclui validações de entrada.
 */
public record ResponsibleRequest(
        @NotBlank
        @Size(min = 2)
        String name,

        @Email
        String email,

        @NotBlank
        String phone,

        @CPF
        // Considere validar o tamanho também se necessário
        String document
) {

    public static Responsible to(ResponsibleRequest dto) {
        return Responsible.builder()
                .name(dto.name())
                .email(dto.email())
                .phone(dto.phone())
                // Definir outros campos padrão se necessário
                .build();
    }

}