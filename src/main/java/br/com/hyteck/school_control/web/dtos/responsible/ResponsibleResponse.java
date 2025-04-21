package br.com.hyteck.school_control.web.dtos.responsible;

import br.com.hyteck.school_control.models.payments.Responsible;

/**
 * DTO para receber os dados de criação de um novo Responsável.
 * Inclui validações de entrada.
 */

/**
 * DTO para retornar os dados de um Responsável após a criação ou consulta.
 */
public record ResponsibleResponse(
        String id,
        String name,
        String email,
        String phone) {
    /**
     * Método factory para converter uma entidade Responsible em um DTO de resposta.
     *
     * @param responsible A entidade Responsible.
     * @return O DTO correspondente.
     */
    public static ResponsibleResponse from(Responsible responsible) {
        if (responsible == null) {
            return null;
        }
        return new ResponsibleResponse(
                responsible.getId(),
                responsible.getName(),
                responsible.getEmail(),
                responsible.getPhone()
        );
    }
}