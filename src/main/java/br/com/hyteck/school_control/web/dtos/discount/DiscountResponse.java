package br.com.hyteck.school_control.web.dtos.discount;

import br.com.hyteck.school_control.models.payments.Discount;
import br.com.hyteck.school_control.models.payments.Types;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record DiscountResponse(
    String id,
    String name,
    String description,
    BigDecimal value,
    LocalDateTime validateAt,
    Types type
) {

    public static DiscountResponse from(Discount discount) {
        return new DiscountResponse(
                discount.getId(),
                discount.getName(),
                discount.getDescription(),
                discount.getValue(),
                discount.getValidateAt(),
                discount.getType()
        );
    }

}
