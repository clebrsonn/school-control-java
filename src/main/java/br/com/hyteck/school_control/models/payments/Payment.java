package br.com.hyteck.school_control.models.payments;

import br.com.hyteck.school_control.models.AbstractModel;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import lombok.*;
import br.com.hyteck.school_control.models.payments.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDateTime; // Pode manter LocalDateTime se a hora do pagamento for importante

@Entity
@Table(name = "payments") // Nome da tabela
@Getter // Lombok
@Setter // Lombok
@NoArgsConstructor // Lombok
@AllArgsConstructor // Lombok
@Builder // Lombok
public class Payment extends AbstractModel {

    // Um pagamento quita UMA fatura específica
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false, unique = true) // FK para Invoice, deve ser única
    @NotNull
    private Invoice invoice;

    @NotNull
    @Positive
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amountPaid; // Valor efetivamente pago

    @NotNull
    @PastOrPresent
    @Column(nullable = false)
    private LocalDateTime paymentDate; // Data e hora do pagamento

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private PaymentStatus status; // Status do pagamento (COMPLETED, FAILED, PENDING)

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private PaymentMethod paymentMethod; // Forma de pagamento (PIX, Boleto, Cartão)

    private String transactionId; // ID da transação (gateway de pagamento, etc.)
    private String description; // Descrição/Observação do pagamento
}
