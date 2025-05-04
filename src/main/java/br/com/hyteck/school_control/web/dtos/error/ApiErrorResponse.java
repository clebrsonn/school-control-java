package br.com.hyteck.school_control.web.dtos.error; // Exemplo de pacote

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record ApiErrorResponse(
        Instant timestamp,
        Integer status,
        String error, // Ex: "Bad Request", "Not Found"
        String message, // Mensagem mais descritiva
        String path,
        Map<String, String> fieldErrors // Opcional, para erros de validação
) {
    // Construtor simplificado para erros gerais
    public ApiErrorResponse(Integer status, String error, String message, String path) {
        this(Instant.now(), status, error, message, path, null);
    }
}