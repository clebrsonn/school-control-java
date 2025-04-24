package br.com.hyteck.school_control.web.controllers; // Ou outro pacote

import br.com.hyteck.school_control.exceptions.DuplicateResourceException;
import br.com.hyteck.school_control.exceptions.ResourceNotFoundException;
import br.com.hyteck.school_control.web.dtos.error.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest; // Para pegar o path
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException; // Segurança
import org.springframework.security.authentication.BadCredentialsException; // Segurança
import org.springframework.security.authentication.DisabledException; // Segurança
import org.springframework.validation.FieldError; // Para erros de validação
import org.springframework.web.bind.MethodArgumentNotValidException; // Validação de @RequestBody
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // --- Erros de Validação ---
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        List<ApiErrorResponse.FieldError> errors = new ArrayList<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.add(new ApiErrorResponse.FieldError(fieldName, errorMessage));
        });

        String message = "Erro de validação. Verifique os campos.";
        logger.warn("Validation error: {} on path {}", message, request.getRequestURI(), ex); // Log menos verboso

        ApiErrorResponse errorResponse = new ApiErrorResponse(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                message,
                request.getRequestURI(),
                errors // Inclui os detalhes dos campos
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    // --- Erros de Negócio Customizados ---
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleResourceNotFoundException(
            ResourceNotFoundException ex, HttpServletRequest request) {

        logger.warn("Resource not found: {} on path {}", ex.getMessage(), request.getRequestURI());
        ApiErrorResponse errorResponse = new ApiErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                HttpStatus.NOT_FOUND.getReasonPhrase(),
                ex.getMessage(), // Mensagem da própria exceção
                request.getRequestURI()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiErrorResponse> handleDuplicateResourceException(
            DuplicateResourceException ex, HttpServletRequest request) {

        logger.warn("Duplicate resource: {} on path {}", ex.getMessage(), request.getRequestURI());
        ApiErrorResponse errorResponse = new ApiErrorResponse(
                HttpStatus.CONFLICT.value(),
                HttpStatus.CONFLICT.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiErrorResponse> handleBusinessRuleException(
            RuntimeException ex, HttpServletRequest request) {

        logger.warn("Business rule violation: {} on path {}", ex.getMessage(), request.getRequestURI());
        ApiErrorResponse errorResponse = new ApiErrorResponse(
                HttpStatus.BAD_REQUEST.value(), // Ou 422 Unprocessable Entity
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    // --- Erros de Segurança (Spring Security) ---
    // Pode ser que o Spring Security já trate isso, mas você pode customizar
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDeniedException(
            AccessDeniedException ex, HttpServletRequest request) {

        logger.warn("Access denied: {} on path {}", ex.getMessage(), request.getRequestURI());
        ApiErrorResponse errorResponse = new ApiErrorResponse(
                HttpStatus.FORBIDDEN.value(),
                HttpStatus.FORBIDDEN.getReasonPhrase(),
                "Acesso negado. Você não tem permissão para acessar este recurso.",
                request.getRequestURI()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler({BadCredentialsException.class})
    public ResponseEntity<ApiErrorResponse> handleBadCredentialsException(
            BadCredentialsException ex, HttpServletRequest request) {

        logger.warn("Authentication failed (Bad Credentials): {} on path {}", ex.getMessage(), request.getRequestURI());
        ApiErrorResponse errorResponse = new ApiErrorResponse(
                HttpStatus.UNAUTHORIZED.value(),
                HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                "Falha na autenticação: Usuário ou senha inválidos.",
                request.getRequestURI()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler({DisabledException.class})
    public ResponseEntity<ApiErrorResponse> handleDisabledException(
            DisabledException ex, HttpServletRequest request) {

        logger.warn("Authentication failed (Account Disabled): {} on path {}", ex.getMessage(), request.getRequestURI());
        ApiErrorResponse errorResponse = new ApiErrorResponse(
                HttpStatus.UNAUTHORIZED.value(), // Ou FORBIDDEN (403)
                HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                "Conta desativada. Por favor, verifique seu e-mail ou contate o suporte.",
                request.getRequestURI()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
    }


    // --- Erro Genérico (Catch-All) ---
    // Deve ser o último handler
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGenericException(
            Exception ex, HttpServletRequest request) {

        // LOG DETALHADO É CRUCIAL AQUI!
        logger.error("An unexpected error occurred on path {}: {}", request.getRequestURI(), ex.getMessage(), ex);

        ApiErrorResponse errorResponse = new ApiErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                "Ocorreu um erro inesperado no servidor. Tente novamente mais tarde.", // Mensagem genérica para o usuário
                request.getRequestURI()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}