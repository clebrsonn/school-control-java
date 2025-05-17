package br.com.hyteck.school_control.web.dtos.discount;

import br.com.hyteck.school_control.models.payments.Discount;
import br.com.hyteck.school_control.models.payments.Types;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record DiscountRequest(
    String name,
    String description,
    BigDecimal value,
    LocalDateTime validateAt,
    Types type
) {
    public Discount to() {
        return Discount.builder()
                .name(name())
                .description(description())
                .value(value())
                .validateAt(validateAt())
                .type(type())
                .build();
    }
}
