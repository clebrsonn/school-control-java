package br.com.hyteck.school_control.web.controllers;

import br.com.hyteck.school_control.models.payments.InvoiceStatus;
import br.com.hyteck.school_control.services.InvoiceCalculationService;
import br.com.hyteck.school_control.usecases.billing.CountInvoicesByStatus;
import br.com.hyteck.school_control.usecases.billing.GenerateConsolidatedStatementUseCase;
import br.com.hyteck.school_control.usecases.billing.GenerateInvoicesForParents;
import br.com.hyteck.school_control.web.dtos.billing.ConsolidatedStatement;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

/**
 * REST controller for billing and invoice operations.
 * Provides endpoints for consolidated statements, invoice generation, and financial summaries.
 */
@Slf4j
@AllArgsConstructor
@RestController
@RequestMapping("/billing")
@Tag(name = "Billing", description = "Endpoints for billing and invoice management")
public class BillingController {

    private final GenerateConsolidatedStatementUseCase generateStatementUseCase;
    private final GenerateInvoicesForParents generateInvoicesForParents;
    private final CountInvoicesByStatus countInvoicesByStatus;
    private final InvoiceCalculationService invoiceCalculationService;

    /**
     * Retrieves the consolidated financial statement for a specific responsible person for a given month.
     *
     * @param responsibleId The ID of the responsible person.
     * @param yearMonth     The year and month for which to generate the statement (format: yyyy-MM).
     * @return A {@link ResponseEntity} containing the {@link ConsolidatedStatement} if found, or 404 Not Found.
     */
    @GetMapping("/responsibles/{responsibleId}/statements/{yearMonth}")
    public ResponseEntity<ConsolidatedStatement> getConsolidatedStatementForResponsible(
            @PathVariable String responsibleId,
            @PathVariable @DateTimeFormat(pattern = "yyyy-MM") YearMonth yearMonth) {

        // Delegate to the use case to generate the statement.
        Optional<ConsolidatedStatement> statementOpt = generateStatementUseCase.execute(responsibleId, yearMonth);

        // Return the statement if present, otherwise return 404 Not Found.
        return statementOpt
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Retrieves a list of consolidated financial statements for all responsible persons for a given month.
     *
     * @param yearMonth The year and month for which to generate the statements (format: yyyy-MM).
     * @return A {@link ResponseEntity} containing a list of {@link ConsolidatedStatement}.
     */
    @GetMapping("/statements/{yearMonth}")
    public ResponseEntity<List<ConsolidatedStatement>> getConsolidatedStatement(
            @PathVariable @DateTimeFormat(pattern = "yyyy-MM") YearMonth yearMonth) {

        // Delegate to the use case to generate statements for the specified month.
        List<ConsolidatedStatement> statements = generateStatementUseCase.execute(yearMonth);

        // Return 200 OK with the list of statements.
        return ResponseEntity.ok(statements);
    }

    /**
     * Triggers the generation of monthly invoices for all applicable parents/responsibles for a given month.
     * This endpoint is restricted to users with the 'ADMIN' role.
     *
     * @param yearMonth The year and month for which to generate invoices (format: yyyy-MM).
     * @return A {@link ResponseEntity} with status 202 Accepted, indicating the process has been initiated.
     */
    @PostMapping("/generate-monthly-invoices/{yearMonth}")
    @PreAuthorize("hasRole('ADMIN')") // Only administrators can trigger this.
    public ResponseEntity<Void> triggerGenerateMonthlyInvoices(
            @PathVariable @DateTimeFormat(pattern = "yyyy-MM") YearMonth yearMonth) {
        // Execute the use case to generate invoices.
        generateInvoicesForParents.execute(yearMonth);
        // Return 202 Accepted, as this is likely an asynchronous process.
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    /**
     * Counts the number of invoices by their status (e.g., PENDING, PAID, OVERDUE).
     * This endpoint is restricted to users with the 'ADMIN' role.
     *
     * @param status The {@link InvoiceStatus} to filter by.
     * @return A {@link ResponseEntity} containing the count of invoices with the specified status.
     */
    @GetMapping("/invoices/{status}/count")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Long> countInvoicesByStatus(@PathVariable InvoiceStatus status) {
        // Delegate to the use case to count invoices by status.
        return ResponseEntity.ok(countInvoicesByStatus.execute(status));
    }

    /**
     * Returns the total amount to be received for all open invoices (PENDING and OVERDUE) in the given month.
     *
     * @param referenceMonth the reference month in yyyy-MM format
     * @return the total amount to be received
     */
    @Operation(
            summary = "Get total amount to be received in a month",
            description = "Returns the sum of all open invoices (PENDING and OVERDUE) for the specified month.",
            parameters = {
                    @Parameter(name = "referenceMonth", description = "Reference month in yyyy-MM format", example = "2025-05")
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Total amount to be received",
                            content = @Content(schema = @Schema(implementation = BigDecimal.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid month format", content = @Content)
            }
    )
    @GetMapping("/total-month/{referenceMonth}")
    public ResponseEntity<BigDecimal> getTotalToReceive(
            @PathVariable("referenceMonth") @DateTimeFormat(pattern = "yyyy-MM") YearMonth referenceMonth) {
        BigDecimal total = invoiceCalculationService.calcularTotalAReceberNoMes(referenceMonth);
        return ResponseEntity.ok(total);
    }


    // --- Endpoint para Processar Pagamento Consolidado (Exemplo de Webhook) ---
    // Este endpoint seria chamado pelo seu gateway de pagamento após a confirmação
    /*
    @PostMapping("/payments/consolidated/webhook")
    public ResponseEntity<Void> processConsolidatedPaymentWebhook(@RequestBody PaymentWebhookPayload payload) {
        // 1. Validar o payload do webhook (segurança é crucial aqui!)
        // 2. Extrair informações relevantes (ID da transação consolidada, valor, status)
        // 3. Chamar um UseCase como 'ProcessConsolidatedPaymentUseCase'
        //    - Este use case buscaria as Invoice IDs originais associadas a esta transação
        //    - Criaria os registros de Payment individuais
        //    - Atualizaria o status das Invoices individuais para PAID
        // 4. Retornar status 200 OK para o gateway confirmar o recebimento
        boolean success = processConsolidatedPaymentUseCase.execute(payload);
        if (success) {
            return ResponseEntity.ok().build();
        } else {
            // Logar erro e talvez retornar um erro diferente, dependendo do gateway
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    */

}
