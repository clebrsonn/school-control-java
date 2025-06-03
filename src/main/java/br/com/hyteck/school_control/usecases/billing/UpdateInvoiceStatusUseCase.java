package br.com.hyteck.school_control.usecases.billing;

import br.com.hyteck.school_control.exceptions.BusinessException;
import br.com.hyteck.school_control.exceptions.ResourceNotFoundException;
import br.com.hyteck.school_control.models.financial.Account;
import br.com.hyteck.school_control.models.payments.Invoice;
import br.com.hyteck.school_control.models.payments.InvoiceStatus;
import br.com.hyteck.school_control.models.payments.Responsible;
import br.com.hyteck.school_control.repositories.InvoiceRepository;
import br.com.hyteck.school_control.repositories.financial.LedgerEntryRepository;
import br.com.hyteck.school_control.events.InvoiceStatusChangedEvent;
import br.com.hyteck.school_control.services.financial.AccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID; // Added for UUID conversion

/**
 * Use case responsible for updating the status of an invoice based on its current
 * ledger balance and due date. Publishes an {@link InvoiceStatusChangedEvent} if the status changes.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UpdateInvoiceStatusUseCase {

    private final InvoiceRepository invoiceRepository;
    private final AccountService accountService;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final ApplicationEventPublisher eventPublisher; // Added
    // Removed ApplyPenaltyUseCase dependency

    /**
     * Updates the status of the specified invoice based on its current ledger balance and due date.
     * Publishes an {@link InvoiceStatusChangedEvent} if the status changes.
     * Does not apply penalties directly.
     *
     * @param invoiceId The ID of the invoice to update.
     * @return The updated {@link Invoice}.
     * @throws ResourceNotFoundException if the invoice or its responsible/user is not found for event publishing.
     * @throws BusinessException if the invoice is already cancelled.
     */
    @Transactional
    public Invoice execute(String invoiceId) {
        log.info("Updating status for Invoice ID: {}", invoiceId);

        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> {
                    log.error("Invoice not found with ID: {} for status update.", invoiceId);
                    return new ResourceNotFoundException("Invoice not found with ID: " + invoiceId);
                });

        if (invoice.getStatus() == InvoiceStatus.CANCELLED) {
            log.warn("Invoice ID: {} is already CANCELLED. No status update will be performed.", invoiceId);
            return invoice; 
        }
        
        Responsible responsible = invoice.getResponsible();
        if (responsible == null || responsible.getId() == null) {
            log.error("Invoice ID: {} has no associated responsible party or responsible user with ID. Cannot update status or publish event.", invoiceId);
            throw new ResourceNotFoundException("Responsible party or user details not found for Invoice ID: " + invoiceId);
        }
        
        // If already PAID, re-verify balance. If balance is no longer <=0 (e.g. refund posted), status might change.
        // If it's still PAID and balance is <=0, no change needed.
        if (invoice.getStatus() == InvoiceStatus.PAID) {
             Account arAccountForPaid = accountService.findOrCreateResponsibleARAccount(responsible.getId());
             BigDecimal balanceForPaid = ledgerEntryRepository.getBalanceForInvoiceOnAccount(arAccountForPaid.getId(), invoice.getId());
             balanceForPaid = (balanceForPaid == null) ? BigDecimal.ZERO : balanceForPaid;
             if (balanceForPaid.compareTo(BigDecimal.ZERO) <= 0) {
                 log.info("Invoice ID: {} is already PAID and balance is still zero or negative. No status change.", invoiceId);
                 return invoice; 
             }
             log.info("Invoice ID: {} was PAID, but balance is now {}. Re-evaluating status.", invoiceId, balanceForPaid);
        }

        Account arAccount = accountService.findOrCreateResponsibleARAccount(responsible.getId());
        BigDecimal currentBalanceOnAR = ledgerEntryRepository.getBalanceForInvoiceOnAccount(arAccount.getId(), invoice.getId());
        currentBalanceOnAR = (currentBalanceOnAR == null) ? BigDecimal.ZERO : currentBalanceOnAR;

        log.debug("Invoice ID: {}, Responsible A/R Account ID: {}, Current Balance on A/R for this invoice: {}",
                invoiceId, arAccount.getId(), currentBalanceOnAR);

        InvoiceStatus oldStatus = invoice.getStatus();
        InvoiceStatus newStatus;

        if (currentBalanceOnAR.compareTo(BigDecimal.ZERO) <= 0) {
            newStatus = InvoiceStatus.PAID;
        } else {
            // Balance is positive, check due date
            if (invoice.getDueDate().isBefore(LocalDate.now(ZoneId.of("America/Sao_Paulo")))) {
                newStatus = InvoiceStatus.OVERDUE;
            } else {
                newStatus = InvoiceStatus.PENDING;
            }
        }

        if (oldStatus != newStatus) {
            invoice.setStatus(newStatus);
            invoiceRepository.save(invoice);
            log.info("Invoice ID: {} status updated from {} to {}. Current Balance on A/R: {}",
                    invoiceId, oldStatus, newStatus, currentBalanceOnAR);

            // Publish InvoiceStatusChangedEvent
            try {
                InvoiceStatusChangedEvent event = new InvoiceStatusChangedEvent(
                        this,
                        UUID.fromString(invoice.getId()),
                        oldStatus,
                        newStatus,
                        UUID.fromString(responsible.getId())
                );
                eventPublisher.publishEvent(event);
                log.info("Published InvoiceStatusChangedEvent for Invoice ID {}: {} -> {}", invoice.getId(), oldStatus, newStatus);
            } catch (IllegalArgumentException e) {
                log.error("Error creating UUID for event publishing related to Invoice ID {}: {}. Ensure IDs are valid UUIDs.", invoice.getId(), e.getMessage());
            }

        } else {
            log.info("Invoice ID: {} status remains {}. Current Balance on A/R: {}",
                    invoiceId, oldStatus, currentBalanceOnAR);
        }

        return invoice;
    }
}
