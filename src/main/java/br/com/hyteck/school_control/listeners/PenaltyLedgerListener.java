package br.com.hyteck.school_control.listeners;

import br.com.hyteck.school_control.events.PenaltyAssessedEvent;
import br.com.hyteck.school_control.models.financial.Account;
import br.com.hyteck.school_control.models.financial.AccountType;
import br.com.hyteck.school_control.models.financial.LedgerEntryType;
import br.com.hyteck.school_control.models.payments.Invoice;
import br.com.hyteck.school_control.models.payments.Responsible;
import br.com.hyteck.school_control.repositories.InvoiceRepository;
import br.com.hyteck.school_control.repositories.ResponsibleRepository;
import br.com.hyteck.school_control.services.financial.AccountService;
import br.com.hyteck.school_control.services.financial.LedgerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
// No specific exception import like ResourceNotFoundException needed unless we want to catch it explicitly
// import br.com.hyteck.school_control.exceptions.ResourceNotFoundException; 

@Component
@RequiredArgsConstructor
@Slf4j
public class PenaltyLedgerListener {

    private final LedgerService ledgerService;
    private final AccountService accountService;
    private final InvoiceRepository invoiceRepository;
    private final ResponsibleRepository responsibleRepository;

    @EventListener
    @Transactional
    public void handlePenaltyAssessedEvent(PenaltyAssessedEvent event) {
        log.info("PenaltyLedgerListener: Received PenaltyAssessedEvent for Invoice ID: {}, Amount: {}, Responsible ID: {}",
                event.getInvoiceId(), event.getPenaltyAmount(), event.getResponsibleId());

        try {
            // Convert UUIDs from event to String for repository lookup.
            String invoiceIdStr = event.getInvoiceId().toString();
            String responsibleIdStr = event.getResponsibleId().toString();

            Invoice invoice = invoiceRepository.findById(invoiceIdStr)
                    .orElseThrow(() -> new RuntimeException("Invoice not found for ID: " + invoiceIdStr)); // Consider specific exception
            
            Responsible responsible = responsibleRepository.findById(responsibleIdStr)
                    .orElseThrow(() -> new RuntimeException("Responsible not found for ID: " + responsibleIdStr)); // Consider specific exception

            // Determine accounts
            Account responsibleARAccount = accountService.findOrCreateResponsibleARAccount(responsible.getId());
            Account penaltyRevenueAccount = accountService.findOrCreateAccount("Penalty Revenue", AccountType.REVENUE, null);

            // Prepare description
            String description = String.format("Penalty assessed for overdue Invoice #%s", invoice.getId());

            // Post transaction
            ledgerService.postTransaction(
                    invoice,
                    null, // No Payment object for a penalty
                    responsibleARAccount,   // Debit A/R
                    penaltyRevenueAccount,  // Credit Penalty Revenue
                    event.getPenaltyAmount(),
                    LocalDateTime.now(), // Or a date from the event if available and more appropriate
                    description,
                    LedgerEntryType.PENALTY_ASSESSED
            );
            log.info("PenaltyLedgerListener: Successfully posted ledger entries for penalty on Invoice ID: {}", event.getInvoiceId());

        } catch (Exception e) {
            log.error("PenaltyLedgerListener: Failed to process PenaltyAssessedEvent for Invoice ID: {}. Error: {}",
                    event.getInvoiceId(), e.getMessage(), e);
            // Re-throw to ensure transaction rollback and allow for further handling if needed
            throw e; 
        }
    }
}
