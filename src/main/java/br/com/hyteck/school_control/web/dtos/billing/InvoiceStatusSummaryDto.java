package br.com.hyteck.school_control.web.dtos.billing;

import br.com.hyteck.school_control.models.payments.InvoiceStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.jackson.Jacksonized;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Represents basic information about a responsible party.
 *
 * @param id   The unique identifier of the responsible party.
 * @param name The name of the responsible party.
 */
record ResponsibleInfo(String id, String name) {
}

/**
 * Represents basic information about an invoice, including its current ledger balance.
 *
 * @param id                The unique identifier of the invoice.
 * @param responsibleId     The ID of the responsible party.
 * @param responsibleName   The name of the responsible party.
 * @param currentBalanceDue The current balance due on the invoice, derived from ledger entries.
 * @param dueDate           The due date of the invoice.
 * @param status            The current status of the invoice.
 */
record InvoiceBasicInfo(
        String id,
        String responsibleId,
        String responsibleName,
        BigDecimal currentBalanceDue,
        LocalDate dueDate,
        InvoiceStatus status
) {
}

/**
 * Data Transfer Object for providing a summary of invoice statuses.
 * This DTO can hold various counts, total balances, and lists of invoices
 * based on their status (Pending, Overdue, Paid).
 */
@Getter
@Setter
@Builder
@Jacksonized // For deserialization with Lombok's builder
public class InvoiceStatusSummaryDto {

    private long totalPendingInvoices;
    private BigDecimal totalPendingBalance;
    private long totalOverdueInvoices;
    private BigDecimal totalOverdueBalance;
    private long totalPaidOnTimeInPeriod;
    private long totalPaidLateInPeriod;
    private List<InvoiceBasicInfo> overdueInvoicesList;
    private List<InvoiceBasicInfo> pendingInvoicesList;
    // Consider adding a field for the period if this DTO instance refers to one
    // private String periodDescription;


    // Default constructor for Jackson or other frameworks if needed
    public InvoiceStatusSummaryDto() {
        // Initialize collections to avoid null pointers if not explicitly set by builder
        this.overdueInvoicesList = List.of();
        this.pendingInvoicesList = List.of();
        // Initialize BigDecimal fields to ZERO
        this.totalPendingBalance = BigDecimal.ZERO;
        this.totalOverdueBalance = BigDecimal.ZERO;
    }

    // All-args constructor for the builder
    public InvoiceStatusSummaryDto(
            long totalPendingInvoices, BigDecimal totalPendingBalance,
            long totalOverdueInvoices, BigDecimal totalOverdueBalance,
            long totalPaidOnTimeInPeriod, long totalPaidLateInPeriod,
            List<InvoiceBasicInfo> overdueInvoicesList, List<InvoiceBasicInfo> pendingInvoicesList) {
        this.totalPendingInvoices = totalPendingInvoices;
        this.totalPendingBalance = totalPendingBalance;
        this.totalOverdueInvoices = totalOverdueInvoices;
        this.totalOverdueBalance = totalOverdueBalance;
        this.totalPaidOnTimeInPeriod = totalPaidOnTimeInPeriod;
        this.totalPaidLateInPeriod = totalPaidLateInPeriod;
        this.overdueInvoicesList = overdueInvoicesList != null ? overdueInvoicesList : List.of();
        this.pendingInvoicesList = pendingInvoicesList != null ? pendingInvoicesList : List.of();
    }
}
