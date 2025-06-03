package br.com.hyteck.school_control.web.dtos.payments;

import br.com.hyteck.school_control.models.payments.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Data Transfer Object (DTO) for initiating a payment for an invoice.
 * This record encapsulates the necessary information to process a payment,
 * including the ID of the invoice being paid, the amount, and the payment method.
 *
 * @param invoiceId     The unique identifier of the invoice for which the payment is being made. Must not be blank.
 * @param amount        The amount being paid. Must not be null and must be a positive value.
 * @param paymentMethod The method used for the payment (e.g., CREDIT_CARD, BANK_TRANSFER). Must not be null.
 */
public record PaymentRequest(
        @NotBlank(message = "Invoice ID cannot be blank.")
        String invoiceId,

        @NotNull(message = "Payment amount cannot be null.")
        @DecimalMin(value = "0.01", message = "Payment amount must be greater than zero.")
        BigDecimal amount,

        @NotNull(message = "Payment method cannot be null.")
        PaymentMethod paymentMethod
) {
    // Records automatically provide a canonical constructor, getters, equals(), hashCode(), and toString().
}
