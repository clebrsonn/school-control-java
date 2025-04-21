package br.com.hyteck.school_control.web.controllers;

import br.com.hyteck.school_control.web.dtos.billing.ConsolidatedStatement;
import br.com.hyteck.school_control.usecases.billing.GenerateConsolidatedStatementUseCase;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;
import java.util.Optional;

@RestController
@RequestMapping("/billing")
public class BillingController {

    private final GenerateConsolidatedStatementUseCase generateStatementUseCase;

    public BillingController(GenerateConsolidatedStatementUseCase generateStatementUseCase) {
        this.generateStatementUseCase = generateStatementUseCase;
    }

    @GetMapping("/responsibles/{responsibleId}/statements/{yearMonth}")
    public ResponseEntity<ConsolidatedStatement> getConsolidatedStatement(
            @PathVariable String responsibleId,
            @PathVariable @DateTimeFormat(pattern = "yyyy-MM") YearMonth yearMonth) { // Recebe ano-mês

        Optional<ConsolidatedStatement> statementOpt = generateStatementUseCase.execute(responsibleId, yearMonth);

        // Retorna 200 OK com o extrato se encontrado, ou 404 Not Found se não houver faturas ou responsável
        return statementOpt
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
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