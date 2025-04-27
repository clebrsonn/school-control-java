package br.com.hyteck.school_control.web.dtos.payments;

import br.com.hyteck.school_control.models.payments.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PaymentRequest(
        @NotBlank String invoiceId,
        @NotNull BigDecimal amount,
        @NotNull PaymentMethod paymentMethod
) {
}
