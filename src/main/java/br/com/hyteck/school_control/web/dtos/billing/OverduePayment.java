package br.com.hyteck.school_control.web.dtos.billing;

import java.math.BigDecimal;
import java.time.LocalDate;

public record OverduePayment(
        String invoiceId,
        String responsibleName,
        String studentName,
        String classroomName,
        BigDecimal amount,
        LocalDate dueDate
) {
}
