package br.com.hyteck.school_control.web.dtos.expenses;

import br.com.hyteck.school_control.models.expenses.Expense;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Data Transfer Object (DTO) for creating or updating an {@link Expense}.
 * This record encapsulates the information needed to define an expense,
 * including its value, date, description, and an optional receipt file.
 *
 * @param value       The monetary value of the expense. Must not be null and must be a positive value (or zero if allowed).
 * @param date        The date when the expense occurred. Must not be null and must be in the past or present.
 * @param description A description of the expense (e.g., "Office Supplies", "Travel for Conference"). Must not be blank.
 * @param receipt     An optional file representing the receipt for the expense. This is not persisted directly in the Expense entity's
 *                    main table but might be handled by a file storage service.
 */
public record ExpenseRequest (
    @NotNull(message = "Expense value cannot be null.")
    @DecimalMin(value = "0.01", message = "Expense value must be greater than zero.")
    BigDecimal value,

    @NotNull(message = "Expense date cannot be null.")
    @PastOrPresent(message = "Expense date must be in the past or present.")
    LocalDate date,

    @NotBlank(message = "Expense description cannot be blank.")
    String description,

    // MultipartFile is for receiving file uploads. It's not directly part of the Expense entity usually.
    // The service layer will handle processing this file (e.g., saving it and storing a path/URL in the entity).
    MultipartFile receipt
){

    /**
     * Static factory method to convert an {@link ExpenseRequest} DTO to an {@link Expense} entity.
     * This method facilitates the mapping from the data transfer object to the domain model.
     * Note: The receipt (MultipartFile) is not directly mapped here as it typically requires
     * separate processing (e.g., file storage) by the service layer.
     *
     * @param expenseRequest The {@link ExpenseRequest} DTO to convert.
     * @return An {@link Expense} entity populated with data from the DTO.
     */
    public static Expense to(ExpenseRequest expenseRequest){
        // Uses the builder pattern from the Expense entity.
        return Expense.builder()
                .description(expenseRequest.description()) // Set expense description.
                .date(expenseRequest.date()) // Set expense date.
                .value(expenseRequest.value()) // Set expense value.
                // The 'receipt' field (MultipartFile) is handled by the service,
                // which might save the file and set a receipt URL/path on the Expense entity.
                .build(); // Build the Expense object.
    }
}
