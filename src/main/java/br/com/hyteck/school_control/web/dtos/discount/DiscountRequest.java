package br.com.hyteck.school_control.web.dtos.discount;

import br.com.hyteck.school_control.models.payments.Discount;
import br.com.hyteck.school_control.models.payments.Types;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Data Transfer Object (DTO) for creating or updating a {@link Discount}.
 * This record encapsulates the information needed to define a discount,
 * such as its name, description, value, validity date, and type.
 *
 * @param name        The name of the discount (e.g., "Sibling Discount", "Early Bird Discount"). Must not be blank.
 * @param description A brief description of the discount and its terms. Can be null.
 * @param value       The monetary value of the discount. Must not be null and must be a positive value.
 * @param validateAt  The date and time until which the discount is valid. Must be in the present or future. Can be null if the discount does not expire.
 * @param type        The type of charge to which this discount applies (e.g., MENSALIDADE, MATRICULA). Must not be null.
 */
public record DiscountRequest(
    @NotBlank(message = "Discount name cannot be blank.")
    String name,

    String description, // Description can be optional

    @NotNull(message = "Discount value cannot be null.")
    @Positive(message = "Discount value must be positive.")
    BigDecimal value,

    @FutureOrPresent(message = "Discount validity date must be in the present or future.")
    LocalDateTime validateAt, // Can be null if discount has no expiration date

    @NotNull(message = "Discount type cannot be null.")
    Types type
) {
    /**
     * Static factory method to convert a {@link DiscountRequest} DTO to a {@link Discount} entity.
     * This method facilitates the mapping from the data transfer object to the domain model.
     *
     * @return A {@link Discount} entity populated with data from this DTO.
     */
    public Discount to() {
        // Uses the builder pattern from the Discount entity.
        return Discount.builder()
                .name(name()) // Set discount name.
                .description(description()) // Set discount description.
                .value(value()) // Set discount value.
                .validateAt(validateAt()) // Set discount validity date.
                .type(type()) // Set discount type.
                .build(); // Build the Discount object.
    }
}
