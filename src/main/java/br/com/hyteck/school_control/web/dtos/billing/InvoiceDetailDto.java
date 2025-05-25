package br.com.hyteck.school_control.web.dtos.billing;

import br.com.hyteck.school_control.models.payments.InvoiceStatus;
import br.com.hyteck.school_control.web.dtos.payments.PaymentResponse; // Assuming PaymentResponse is suitable
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * Data Transfer Object for returning comprehensive details of a single invoice,
 * including ledger-derived financial summaries.
 */
@Getter
@Builder
@Jacksonized // For deserialization with Lombok's builder
public class InvoiceDetailDto {

    private final String id;
    private final String responsibleId;
    private final String responsibleName;
    private final YearMonth referenceMonth;
    private final LocalDate issueDate;
    private final LocalDate dueDate;
    private final InvoiceStatus status;
    /**
     * The net amount of the invoice, calculated as the sum of all its {@link InvoiceItemDetailDto} amounts.
     * This includes positive amounts for charges (e.g., tuition) and negative amounts for itemized discounts.
     * This value comes directly from {@code Invoice.originalAmount} after itemized discounts are applied.
     */
    private final BigDecimal originalAmount;
    /**
     * Sum of ad-hoc discounts applied directly via {@code LedgerEntryType.DISCOUNT_APPLIED} ledger entries
     * for this invoice on the A/R account. This does NOT include itemized discounts which are already
     * reflected in the {@code originalAmount} and the {@code items} list.
     */
    private final BigDecimal totalAdHocDiscountsApplied;
    /**
     * Sum of penalties assessed directly via {@code LedgerEntryType.PENALTY_ASSESSED} ledger entries
     * for this invoice on the A/R account.
     */
    private final BigDecimal totalPenaltiesAssessed;
    /**
     * Sum of payments received, from {@code LedgerEntryType.PAYMENT_RECEIVED} ledger entries
     * for this invoice on the A/R account.
     */
    private final BigDecimal totalPaymentsReceived;
    /**
     * The current net balance due on the invoice, derived directly from the A/R ledger account for this invoice.
     * This reflects (sum of all debits - sum of all credits) on the A/R account for this specific invoice.
     * It incorporates the net originalAmount, any ad-hoc discounts, penalties, and payments.
     */
    private final BigDecimal currentBalanceDue;
    private final List<InvoiceItemDetailDto> items; // Includes regular items and itemized discount (negative amount) items
    private final List<PaymentResponse> payments; // List of payments made against this invoice

    // Constructor for Lombok's builder
    public InvoiceDetailDto(
            String id, String responsibleId, String responsibleName, YearMonth referenceMonth,
            LocalDate issueDate, LocalDate dueDate, InvoiceStatus status, BigDecimal originalAmount,
            BigDecimal totalAdHocDiscountsApplied, BigDecimal totalPenaltiesAssessed, BigDecimal totalPaymentsReceived,
            BigDecimal currentBalanceDue, List<InvoiceItemDetailDto> items, List<PaymentResponse> payments
    ) {
        this.id = id;
        this.responsibleId = responsibleId;
        this.responsibleName = responsibleName;
        this.referenceMonth = referenceMonth;
        this.issueDate = issueDate;
        this.dueDate = dueDate;
        this.status = status;
        this.originalAmount = originalAmount; // Net amount from invoice items
        this.totalAdHocDiscountsApplied = totalAdHocDiscountsApplied;
        this.totalPenaltiesAssessed = totalPenaltiesAssessed;
        this.totalPaymentsReceived = totalPaymentsReceived;
        this.currentBalanceDue = currentBalanceDue;
        this.items = items != null ? items : List.of();
        this.payments = payments != null ? payments : List.of();
    }
}
