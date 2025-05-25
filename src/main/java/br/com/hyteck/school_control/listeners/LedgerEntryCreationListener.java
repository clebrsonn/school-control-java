package br.com.hyteck.school_control.listeners;

import br.com.hyteck.school_control.events.InvoiceCreatedEvent;
import br.com.hyteck.school_control.models.financial.Account;
import br.com.hyteck.school_control.models.financial.AccountType;
import br.com.hyteck.school_control.models.financial.LedgerEntryType;
import br.com.hyteck.school_control.models.payments.Invoice;
import br.com.hyteck.school_control.models.payments.Responsible;
import br.com.hyteck.school_control.models.payments.Types; // Added import
import br.com.hyteck.school_control.services.financial.AccountService;
import br.com.hyteck.school_control.services.financial.LedgerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor // Generates constructor with final fields
@Slf4j
public class LedgerEntryCreationListener {

    private final LedgerService ledgerService;
    private final AccountService accountService;

    @EventListener
    @Transactional
    public void handleInvoiceCreatedEvent(InvoiceCreatedEvent event) {
        Invoice invoice = event.getInvoice();
        log.info("Received InvoiceCreatedEvent for invoice ID: {}", invoice.getId());

        if (invoice.getResponsible() == null) {
            log.warn("Invoice {} has no responsible party, skipping ledger entry creation.", invoice.getId());
            return;
        }

        if (invoice.getItems() == null || invoice.getItems().isEmpty()) {
            log.warn("Invoice {} has no items, skipping ledger entry creation.", invoice.getId());
            return;
        }

        Responsible responsible = invoice.getResponsible();
        Account debitAccount = accountService.findOrCreateResponsibleARAccount(responsible.getId());

        String creditAccountName;
        LedgerEntryType ledgerEntryType;
        String baseDescription;

        boolean isEnrollmentFee = invoice.getItems().stream()
                                    .anyMatch(item -> item.getType() == Types.MATRICULA);
        boolean isMonthlyFee = invoice.getItems().stream()
                                   .anyMatch(item -> item.getType() == Types.MENSALIDADE);

        if (isEnrollmentFee) {
            creditAccountName = "Enrollment Fee Revenue";
            ledgerEntryType = LedgerEntryType.ENROLLMENT_FEE_CHARGED;
            // Try to get student name for description, specific to enrollment
            String studentName = "N/A";
            try {
                // Ensure that items, enrollment, and student are not null before accessing them
                if (!invoice.getItems().isEmpty() && 
                    invoice.getItems().get(0) != null && // Check item itself is not null
                    invoice.getItems().get(0).getEnrollment() != null &&
                    invoice.getItems().get(0).getEnrollment().getStudent() != null &&
                    invoice.getItems().get(0).getEnrollment().getStudent().getName() != null) {
                    studentName = invoice.getItems().get(0).getEnrollment().getStudent().getName();
                } else if (!invoice.getItems().isEmpty()) { // Log if some part of the chain is null
                    log.warn("Could not retrieve full student name details for enrollment fee invoice {} from first item. Parts might be null.", invoice.getId());
                }
            } catch (IndexOutOfBoundsException e) { // Should be caught by !invoice.getItems().isEmpty()
                 log.warn("Could not retrieve student name for enrollment fee invoice {} because items list is empty.", invoice.getId());
            } catch (Exception e) {
                log.warn("Could not retrieve student name for enrollment fee invoice {}: {}", invoice.getId(), e.getMessage());
            }
            baseDescription = String.format("Enrollment fee for student: %s - Invoice: %s", studentName, invoice.getId());
        } else if (isMonthlyFee) {
            creditAccountName = "Tuition Revenue";
            ledgerEntryType = LedgerEntryType.TUITION_FEE;
            baseDescription = String.format("Monthly tuition fee - Invoice: %s - Ref: %s", invoice.getId(), invoice.getReferenceMonth() != null ? invoice.getReferenceMonth().toString() : "N/A");
        } else {
            log.warn("Invoice ID {} contains items of undetermined type for specific ledger posting. Defaulting to Tuition Revenue.", invoice.getId());
            creditAccountName = "Tuition Revenue"; // Default fallback
            ledgerEntryType = LedgerEntryType.TUITION_FEE; // Default fallback
            baseDescription = String.format("Invoice charges - Invoice: %s - Ref: %s", invoice.getId(), invoice.getReferenceMonth() != null ? invoice.getReferenceMonth().toString() : "N/A");
        }

        Account creditAccount = accountService.findOrCreateAccount(creditAccountName, AccountType.REVENUE, null);

        try {
            ledgerService.postTransaction(
                    invoice,
                    null, // No payment associated
                    debitAccount,
                    creditAccount,
                    invoice.getAmount(), // This is the total net amount of the invoice
                    LocalDateTime.now(),
                    baseDescription,
                    ledgerEntryType
            );
            log.info("Successfully created ledger entries for {} for invoice ID: {}", ledgerEntryType, invoice.getId());
        } catch (Exception e) {
            log.error("Failed to create ledger entries for invoice ID: {}. Error: {}", invoice.getId(), e.getMessage(), e);
            // Transaction should roll back due to the exception
        }
    }
}
