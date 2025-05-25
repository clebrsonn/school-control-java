package br.com.hyteck.school_control.web.dtos.payments;

import br.com.hyteck.school_control.models.payments.Payment;
import br.com.hyteck.school_control.models.payments.PaymentMethod;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Data Transfer Object (DTO) for returning details of a {@link Payment} to clients.
 * This class provides a structured representation of a payment's information.
 * Lombok's {@link Getter} annotation generates getter methods for all fields.
 * Lombok's {@link Builder} annotation provides a builder pattern for constructing instances.
 */
@Getter
@Builder
public class PaymentResponse {
    /**
     * The unique identifier of the payment.
     */
    private String id;

    /**
     * The amount that was paid.
     */
    private BigDecimal amount;

    /**
     * The date and time when the payment was made or processed.
     */
    private LocalDateTime paymentDate;

    /**
     * The method used for the payment (e.g., CREDIT_CARD, BANK_TRANSFER).
     */
    private PaymentMethod paymentMethod;

    /**
     * The unique identifier of the invoice to which this payment is related.
     */
    private String invoiceId;

    /**
     * Static factory method to create a {@link PaymentResponse} from a {@link Payment} entity.
     * This method handles the conversion from the domain model to the DTO.
     *
     * @param payment The {@link Payment} entity to convert.
     * @return A {@link PaymentResponse} populated with data from the Payment entity,
     *         or {@code null} if the input payment is {@code null}.
     */
    public static PaymentResponse from(Payment payment) {
        // Prevent NullPointerException if the input entity is null.
        if (payment == null) {
            return null;
        }
        // Use the builder to create and return a new PaymentResponse.
        return PaymentResponse.builder()
                .id(payment.getId()) // Map the payment's unique ID.
                .amount(payment.getAmountPaid()) // Map the amount paid.
                .paymentDate(payment.getPaymentDate()) // Map the payment date.
                .paymentMethod(payment.getPaymentMethod()) // Map the payment method.
                .invoiceId(payment.getInvoice() != null ? payment.getInvoice().getId() : null) // Map the related invoice ID, handling potential null invoice.
                .build();
    }
}
