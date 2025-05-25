package br.com.hyteck.school_control.web.dtos.billing;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Data Transfer Object (DTO) representing an overdue payment.
 * This record is used to convey information about payments that have passed their due date.
 *
 * @param invoiceId       The unique identifier of the overdue invoice.
 * @param responsibleName The name of the responsible party for the payment.
 * @param studentName     The name of the student associated with the overdue payment.
 * @param classroomName   The name of the classroom associated with the student/enrollment.
 * @param amount          The outstanding amount of the overdue payment.
 * @param dueDate         The date when the payment was due.
 */
public record OverduePayment(
        String invoiceId,
        String responsibleName,
        String studentName,
        String classroomName,
        BigDecimal amount,
        LocalDate dueDate
) {
    // Records automatically generate a canonical constructor, getters, equals(), hashCode(), and toString().
}
