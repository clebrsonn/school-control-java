package br.com.hyteck.school_control.web.dtos.invoice;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Data
@NoArgsConstructor
public class InvoiceItemUpdateRequestDto {

    @NotNull(message = "New amount cannot be null")
    @Positive(message = "New amount must be positive")
    private BigDecimal newAmount;
}
