package br.com.hyteck.school_control.web.dtos.invoice;

import br.com.hyteck.school_control.models.payments.Types;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceItemResponseDto {
    private String id;
    private String description;
    private BigDecimal amount;
    private Types type;
    private String enrollmentId; // Assuming enrollment.getId() will be used
}
