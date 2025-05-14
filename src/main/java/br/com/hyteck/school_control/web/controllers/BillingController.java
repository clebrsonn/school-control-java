package br.com.hyteck.school_control.web.controllers;

import br.com.hyteck.school_control.models.payments.InvoiceStatus;
import br.com.hyteck.school_control.usecases.billing.CountInvoicesByStatus;
import br.com.hyteck.school_control.usecases.billing.GenerateConsolidatedStatementUseCase;
import br.com.hyteck.school_control.usecases.billing.GenerateInvoicesForParents;
import br.com.hyteck.school_control.web.dtos.billing.ConsolidatedStatement;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/billing")
public class BillingController {

    private final GenerateConsolidatedStatementUseCase generateStatementUseCase;
    private final GenerateInvoicesForParents generateInvoicesForParents;
    private final CountInvoicesByStatus countInvoicesByStatus;

    public BillingController(GenerateConsolidatedStatementUseCase generateStatementUseCase, GenerateInvoicesForParents generateInvoicesForParents, CountInvoicesByStatus countInvoicesByStatus) {
        this.generateStatementUseCase = generateStatementUseCase;
        this.generateInvoicesForParents = generateInvoicesForParents;
        this.countInvoicesByStatus = countInvoicesByStatus;
    }

    @GetMapping("/responsibles/{responsibleId}/statements/{yearMonth}")
    public ResponseEntity<ConsolidatedStatement> getConsolidatedStatementForResponsible(
            @PathVariable String responsibleId,
            @PathVariable @DateTimeFormat(pattern = "yyyy-MM") YearMonth yearMonth) { // Recebe ano-mês

        Optional<ConsolidatedStatement> statementOpt = generateStatementUseCase.execute(responsibleId, yearMonth);

        // Retorna 200 OK com o extrato se encontrado, ou 404 Not Found se não houver faturas ou responsável
        return statementOpt
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
    @GetMapping("/statements/{yearMonth}")
    public ResponseEntity<List<ConsolidatedStatement>> getConsolidatedStatement(
            @PathVariable @DateTimeFormat(pattern = "yyyy-MM") YearMonth yearMonth) { // Recebe ano-mês

        List<ConsolidatedStatement> statements = generateStatementUseCase.execute(yearMonth);

        // Retorna 200 OK com o extrato se encontrado, ou 404 Not Found se não houver faturas ou responsável
        return ResponseEntity.ok(statements);
    }

    @PostMapping("/generate-monthly-invoices/{yearMonth}")
    @PreAuthorize("hasRole('ADMIN')") // Apenas administradores podem disparar
    public ResponseEntity<Void> triggerGenerateMonthlyInvoices(
            @PathVariable @DateTimeFormat(pattern = "yyyy-MM") YearMonth yearMonth) {
            generateInvoicesForParents.execute(yearMonth);
            return ResponseEntity.status(HttpStatus.ACCEPTED).build(); // 202 Accepted - processo iniciado
    }

    @GetMapping("/invoices/{status}/count")
    @PreAuthorize("hasRole('ADMIN')") // Apenas administradores podem disparar
    public ResponseEntity<Long> countInvoicesByStatus(@PathVariable InvoiceStatus status){
        return ResponseEntity.ok(countInvoicesByStatus.execute(status));
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