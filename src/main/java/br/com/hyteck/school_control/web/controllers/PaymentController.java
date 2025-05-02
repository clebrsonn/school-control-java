package br.com.hyteck.school_control.web.controllers;

import br.com.hyteck.school_control.models.payments.Payment;
import br.com.hyteck.school_control.usecases.billing.FindPaymentById;
import br.com.hyteck.school_control.usecases.billing.ProcessPaymentUseCase;
import br.com.hyteck.school_control.web.dtos.classroom.ClassRoomResponse;
import br.com.hyteck.school_control.web.dtos.payments.PaymentRequest;
import br.com.hyteck.school_control.web.dtos.payments.PaymentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/payments")
@Tag(name = "Payments", description = "API para gerenciamento de pagamentos")
public class PaymentController {

    private final ProcessPaymentUseCase processPaymentUseCase;
    private final FindPaymentById findPaymentById;

    public PaymentController(ProcessPaymentUseCase processPaymentUseCase, FindPaymentById findPaymentById) {
        this.processPaymentUseCase = processPaymentUseCase;
        this.findPaymentById = findPaymentById;
    }

    @PostMapping
    @Operation(summary = "Processar pagamento",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Pagamento processado com sucesso"),
                    @ApiResponse(responseCode = "400", description = "Dados inválidos"),
                    @ApiResponse(responseCode = "404", description = "Fatura não encontrada")
            })
    public PaymentResponse processPayment(@Valid @RequestBody PaymentRequest request) {
        Payment payment = processPaymentUseCase.execute(request.invoiceId(), request.amount(), request.paymentMethod());
        return PaymentResponse.from(payment);
    }

    // --- READ (Single) ---
    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponse> getPaymentById(@PathVariable String id) {
        return findPaymentById.execute(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
