package br.com.hyteck.school_control.usecases.billing;

import br.com.hyteck.school_control.events.PenaltyAssessedEvent;
import br.com.hyteck.school_control.exceptions.BusinessException;
import br.com.hyteck.school_control.exceptions.ResourceNotFoundException;
import br.com.hyteck.school_control.models.financial.Account;
import br.com.hyteck.school_control.models.financial.AccountType;
import br.com.hyteck.school_control.models.financial.LedgerEntryType;
import br.com.hyteck.school_control.models.payments.Invoice;
import br.com.hyteck.school_control.models.payments.InvoiceStatus;
import br.com.hyteck.school_control.models.payments.Responsible;
import br.com.hyteck.school_control.repositories.InvoiceRepository;
import br.com.hyteck.school_control.services.financial.AccountService;
import br.com.hyteck.school_control.services.financial.LedgerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

/**
 * Use case responsible for applying penalties to overdue invoices.
 * It posts corresponding entries to the financial ledger and publishes a {@link PenaltyAssessedEvent}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ApplyPenaltyUseCase {

    private final InvoiceRepository invoiceRepository;
    private final AccountService accountService;
    private final LedgerService ledgerService;
    private final ApplicationEventPublisher eventPublisher; // Added

    // Define the penalty amount, could be configurable
    private static final BigDecimal PENALTY_AMOUNT = new BigDecimal("10.00");
    private static final String PENALTY_REVENUE_ACCOUNT_NAME = "Penalty Revenue";
    private static final String PENALTY_DESCRIPTION_PREFIX = "Penalty assessed for overdue Invoice #";


    /**
     * Applies a penalty to the specified invoice if it's overdue and eligible.
     *
     * @param invoiceId The ID of the invoice to apply the penalty to.
     * @throws ResourceNotFoundException if the invoice is not found.
     * @throws BusinessException if the invoice is not in a state eligible for penalty.
     */
    @Transactional
    public void execute(String invoiceId) {
        log.info("Attempting to apply penalty to Invoice ID: {}", invoiceId);

        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> {
                    log.error("Invoice not found with ID: {} for penalty application.", invoiceId);
                    return new ResourceNotFoundException("Invoice not found with ID: " + invoiceId);
                });

        // Check if invoice is eligible for penalty
        // Typically, an invoice should be OVERDUE or PENDING and past its due date.
        boolean isPastDueDate = invoice.getDueDate().isBefore(LocalDate.now(ZoneId.of("America/Sao_Paulo")));
        boolean isEligibleStatus = invoice.getStatus() == InvoiceStatus.OVERDUE ||
                                   (invoice.getStatus() == InvoiceStatus.PENDING && isPastDueDate);

        if (!isEligibleStatus) {
            log.warn("Invoice ID: {} is not eligible for penalty. Status: {}, Due Date: {}",
                    invoiceId, invoice.getStatus(), invoice.getDueDate());
            throw new BusinessException("Invoice ID: " + invoiceId + " is not eligible for penalty application. " +
                                        "Status: " + invoice.getStatus() + ", Due Date: " + invoice.getDueDate());
        }
        
        // If the status is PENDING but it's past due, update it to OVERDUE first
        if (invoice.getStatus() == InvoiceStatus.PENDING && isPastDueDate) {
            log.info("Invoice ID: {} is PENDING and past due date. Updating status to OVERDUE before applying penalty.", invoiceId);
            invoice.setStatus(InvoiceStatus.OVERDUE);
            // Note: The invoice balance check is separate. This just marks it overdue.
            // The invoiceRepository.save(invoice) will be called implicitly by the ledger posting if it's part of the same transaction,
            // or explicitly if needed after this status change.
        }


        Responsible responsible = invoice.getResponsible();
        if (responsible == null || responsible.getId() == null) {
            log.error("Invoice ID: {} has no associated responsible party or user ID. Cannot apply penalty or publish event.", invoiceId);
            throw new BusinessException("Invoice responsible or user details not found. Cannot apply penalty.");
        }

        Account arAccount = accountService.findOrCreateResponsibleARAccount(responsible.getId());
        Account penaltyRevenueAccount = accountService.findOrCreateAccount(PENALTY_REVENUE_ACCOUNT_NAME, AccountType.REVENUE, null);

        // Check if a penalty of this type was already applied to this invoice to prevent duplicates
        // This check might need to be more sophisticated, e.g., checking ledger entries for this invoice with PENALTY_ASSESSED type.
        // For now, we assume one penalty application is triggered when due.

        log.info("Applying penalty of {} to Invoice ID: {} for Responsible: {}", PENALTY_AMOUNT, invoiceId, responsible.getName());

        ledgerService.postTransaction(
                invoice,
                null, // No direct payment associated with penalty assessment itself
                arAccount,                // Debit A/R (increase amount due from responsible)
                penaltyRevenueAccount,    // Credit Penalty Revenue
                PENALTY_AMOUNT,
                LocalDateTime.now(ZoneId.of("America/Sao_Paulo")),
                PENALTY_DESCRIPTION_PREFIX + invoice.getId(),
                LedgerEntryType.PENALTY_ASSESSED
        );
        
        // The ledgerService.postTransaction already updates the balances on arAccount.
        // No need to explicitly call accountService.updateAccountBalance(arAccount.getId()) here again.
        // Save the invoice if its status was changed.
        invoiceRepository.save(invoice);

        log.info("Penalty successfully applied to Invoice ID: {}. A/R account {} debited, Penalty Revenue account {} credited.",
                invoiceId, arAccount.getName(), penaltyRevenueAccount.getName());
        
        // Publish PenaltyAssessedEvent
        try {
            PenaltyAssessedEvent event = new PenaltyAssessedEvent(
                    this,
                    UUID.fromString(invoice.getId()),
                    PENALTY_AMOUNT, // Use the actual penalty amount applied
                    UUID.fromString(responsible.getId())
            );
            eventPublisher.publishEvent(event);
            log.info("Published PenaltyAssessedEvent for Invoice ID {}", invoice.getId());
        } catch (IllegalArgumentException e) {
            log.error("Error creating UUID for event publishing related to Invoice ID {}: {}. Ensure IDs are valid UUIDs.", invoice.getId(), e.getMessage());
        }
    }
}
