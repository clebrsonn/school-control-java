package br.com.hyteck.school_control.web.dtos.billing;

import java.math.BigDecimal; /**
 * Represents detailed information about a single invoice item.
 *
 * @param description    Description of the invoice item.
 * @param amount         The individual amount of this item.
 * @param enrollmentInfo A string representation of enrollment details (e.g., "Student Name - Classroom Name").
 * @param type           The type of the invoice item (e.g., TUITION_FEE, ENROLLMENT_FEE).
 */
public record InvoiceItemDetailDto(
        String id, // Item ID
        String description,
        BigDecimal amount,
        String enrollmentInfo, // Simplified, could be more structured
        String type // e.g., from Types.MENSALIDADE.toString()
) {
}
