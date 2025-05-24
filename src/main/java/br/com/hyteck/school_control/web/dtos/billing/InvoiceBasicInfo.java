package br.com.hyteck.school_control.web.dtos.billing;

import br.com.hyteck.school_control.models.payments.InvoiceStatus;

import java.math.BigDecimal;
import java.time.LocalDate; /**
 * Represents basic information about an invoice, including its current ledger balance.
 *
 * @param id                The unique identifier of the invoice.
 * @param responsibleId     The ID of the responsible party.
 * @param responsibleName   The name of the responsible party.
 * @param currentBalanceDue The current balance due on the invoice, derived from ledger entries.
 * @param dueDate           The due date of the invoice.
 * @param status            The current status of the invoice.
 */
public record InvoiceBasicInfo(
        String id,
        String responsibleId,
        String responsibleName,
        BigDecimal currentBalanceDue,
        LocalDate dueDate,
        InvoiceStatus status
) {
}
