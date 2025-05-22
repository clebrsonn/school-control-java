package br.com.hyteck.school_control.web.dtos.discount;

import br.com.hyteck.school_control.models.payments.Discount;
import br.com.hyteck.school_control.models.payments.Types;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Data Transfer Object (DTO) for returning details of a {@link Discount} to clients.
 * This record provides a structured representation of a discount's information.
 *
 * @param id          The unique identifier of the discount.
 * @param name        The name of the discount (e.g., "Sibling Discount", "Early Bird Discount").
 * @param description A brief description of the discount and its terms.
 * @param value       The monetary value of the discount.
 * @param validateAt  The date and time until which the discount is valid. Can be null if the discount does not expire.
 * @param type        The type of charge to which this discount applies (e.g., MENSALIDADE, MATRICULA).
 */
public record DiscountResponse(
    String id,
    String name,
    String description,
    BigDecimal value,
    LocalDateTime validateAt,
    Types type
) {

    /**
     * Static factory method to create a {@link DiscountResponse} from a {@link Discount} entity.
     * This method handles the conversion from the domain model to the DTO.
     *
     * @param discount The {@link Discount} entity to convert.
     * @return A {@link DiscountResponse} populated with data from the Discount entity,
     *         or {@code null} if the input discount is {@code null}.
     */
    public static DiscountResponse from(Discount discount) {
        // Prevent NullPointerException if the input entity is null.
        if (discount == null) {
            return null;
        }
        // Create and return a new DiscountResponse, mapping fields from the entity.
        return new DiscountResponse(
                discount.getId(), // Map the discount's unique ID.
                discount.getName(), // Map the discount's name.
                discount.getDescription(), // Map the discount's description.
                discount.getValue(), // Map the discount's value.
                discount.getValidateAt(), // Map the discount's validity date.
                discount.getType() // Map the discount's type.
        );
    }

}
