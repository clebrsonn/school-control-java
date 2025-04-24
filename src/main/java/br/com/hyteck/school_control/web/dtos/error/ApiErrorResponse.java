package br.com.hyteck.school_control.web.dtos.error; // Exemplo de pacote

import java.time.Instant;
import java.util.List;

public record ApiErrorResponse(
        Instant timestamp,
        Integer status,
        String error, // Ex: "Bad Request", "Not Found"
        String message, // Mensagem mais descritiva
        String path,
        List<FieldError> fieldErrors // Opcional, para erros de validação
) {
    // Construtor simplificado para erros gerais
    public ApiErrorResponse(Integer status, String error, String message, String path) {
        this(Instant.now(), status, error, message, path, null);
    }

    // DTO interno para erros de campo
    public record FieldError(String field, String message) {}
}