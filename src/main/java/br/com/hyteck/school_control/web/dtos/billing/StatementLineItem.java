package br.com.hyteck.school_control.web.dtos.billing;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Represents a single line item within a {@link ConsolidatedStatement}.
 * Each line item typically corresponds to an individual charge or invoice,
 * such as a monthly fee for a specific student. This record is immutable.
 *
 * @param invoiceId     The unique identifier of the original invoice this line item represents.
 * @param studentName   The name of the student associated with this specific charge.
 * @param description   A description of the charge (e.g., "Monthly Fee Class X - August/2024", "Enrollment Fee").
 * @param amount        The amount of this individual line item.
 * @param dueDate       The due date for this specific line item (which might differ from the overall statement due date if not consolidated).
 */
public record StatementLineItem(
        String invoiceId,
        String studentName,
        String description,
        BigDecimal amount,
        LocalDate dueDate
) {
    // Records automatically generate a canonical constructor, public accessor methods for all fields,
    // as well as equals(), hashCode(), and toString() methods.
}