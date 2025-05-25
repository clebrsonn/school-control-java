package br.com.hyteck.school_control.usecases.billing;

import br.com.hyteck.school_control.events.PaymentProcessedEvent;
import br.com.hyteck.school_control.exceptions.BusinessException;
import br.com.hyteck.school_control.exceptions.ResourceNotFoundException;
import br.com.hyteck.school_control.models.financial.Account; // Still needed for arAccount for balance check
// import br.com.hyteck.school_control.models.financial.AccountType; // Removed
// import br.com.hyteck.school_control.models.financial.LedgerEntryType; // Removed
import br.com.hyteck.school_control.models.payments.*;
import br.com.hyteck.school_control.repositories.InvoiceRepository;
import br.com.hyteck.school_control.repositories.PaymentRepository;
import br.com.hyteck.school_control.repositories.financial.LedgerEntryRepository;
import br.com.hyteck.school_control.services.financial.AccountService; // Still needed for arAccount for balance check
// import br.com.hyteck.school_control.services.financial.LedgerService; // Removed
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProcessPaymentUseCase {

    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final AccountService accountService; // Still needed for arAccount for balance check
    // private final LedgerService ledgerService; // Removed
    private final LedgerEntryRepository ledgerEntryRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Payment execute(String invoiceId, BigDecimal amount, PaymentMethod paymentMethod) {
        log.info("Processing payment for Invoice ID: {}, Amount: {}, Method: {}", invoiceId, amount, paymentMethod);

        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> {
                    log.error("Invoice not found with ID: {}", invoiceId);
                    return new ResourceNotFoundException("Invoice not found with ID: " + invoiceId);
                });

        if (invoice.getStatus() == InvoiceStatus.PAID || invoice.getStatus() == InvoiceStatus.CANCELLED) {
            log.warn("Attempt to pay an already PAID or CANCELLED invoice. Invoice ID: {}, Status: {}", invoiceId, invoice.getStatus());
            throw new BusinessException("Invoice is already " + invoice.getStatus().name().toLowerCase() + ".");
        }
        
        Responsible responsible = invoice.getResponsible();
        if (responsible == null || responsible.getId() == null) {
            log.error("Invoice ID: {} does not have an associated responsible party or responsible user with ID.", invoiceId);
            throw new BusinessException("Invoice responsible party or user details not found. Cannot process payment or publish event.");
        }

        // Create and save the Payment record first
        Payment payment = Payment.builder()
                .amountPaid(amount)
                .paymentDate(LocalDateTime.now(ZoneId.of("America/Sao_Paulo")))
                .paymentMethod(paymentMethod)
                .invoice(invoice)
                .status(PaymentStatus.PENDING_CONFIRMATION) // Initial status
                .build();
        Payment savedPayment = paymentRepository.save(payment);
        log.info("Payment record created with ID: {} for Invoice ID: {}", savedPayment.getId(), invoiceId);

        // arAccount is still needed for balance checking
        Account arAccount = accountService.findOrCreateResponsibleARAccount(responsible.getId());
        // Removed: Account cashClearingAccount = accountService.findOrCreateAccount("Cash/Bank Clearing", AccountType.ASSET, null);

        // Removed: Direct ledger posting call
        // ledgerService.postTransaction(...)

        // Update invoice status based on its new balance on the A/R account.
        // Note: This balance is now read *before* the PaymentLedgerListener updates the ledger for this payment.
        // To make the status update reflect the current payment's effect, we adjust the fetched balance.
        BigDecimal invoiceBalanceOnAR = ledgerEntryRepository.getBalanceForInvoiceOnAccount(arAccount.getId(), invoice.getId());
        invoiceBalanceOnAR = (invoiceBalanceOnAR == null) ? BigDecimal.ZERO : invoiceBalanceOnAR;
        
        BigDecimal effectiveBalanceForStatusCheck = invoiceBalanceOnAR.subtract(amount);
        log.info("Balance for Invoice ID {} on A/R Account {} is: {}. Current payment: {}. Effective balance for status update: {}",
                 invoice.getId(), arAccount.getName(), invoiceBalanceOnAR, amount, effectiveBalanceForStatusCheck);
        
        // Use effectiveBalanceForStatusCheck for subsequent logic
        if (effectiveBalanceForStatusCheck.compareTo(BigDecimal.ZERO) <= 0) {
            // If balance is zero or negative (overpayment), mark as PAID
            invoice.setStatus(InvoiceStatus.PAID);
            savedPayment.setStatus(PaymentStatus.COMPLETED);
            log.info("Invoice ID: {} marked as PAID. Payment ID: {} marked as COMPLETED.", invoiceId, savedPayment.getId());
        } else {
            // Partially paid
            savedPayment.setStatus(PaymentStatus.COMPLETED); // Payment itself is completed
            log.info("Invoice ID: {} is partially paid. Effective balance on A/R after this payment: {}. Payment ID: {} marked as COMPLETED.",
                    invoiceId, effectiveBalanceForStatusCheck, savedPayment.getId());
            if (invoice.getDueDate().isBefore(LocalDate.now(ZoneId.of("America/Sao_Paulo"))) && invoice.getStatus() != InvoiceStatus.PAID) {
                invoice.setStatus(InvoiceStatus.OVERDUE);
                 log.info("Invoice ID: {} is past due date and not fully paid, marked as OVERDUE.", invoiceId);
            }
        }
        
        // Associate payment with invoice (if not already done by builder, good to be explicit)
        invoice.setPayment(savedPayment);
        invoiceRepository.save(invoice);
        Payment finalPayment = paymentRepository.save(savedPayment); // Save payment again if status changed

        // Publish PaymentProcessedEvent
        try {
            PaymentProcessedEvent eventData = new PaymentProcessedEvent(
                    this,
                    finalPayment.getId(),
                    invoice.getId(),
                    finalPayment.getAmountPaid(),
                    finalPayment.getStatus(),
                    invoice.getStatus(),
                    responsible.getId()
            );
            eventPublisher.publishEvent(eventData);
            log.info("Published PaymentProcessedEvent for Payment ID: {}", finalPayment.getId());
        } catch (IllegalArgumentException e) {
            log.error("Error creating UUID for event publishing related to Payment ID {}: {}. Ensure IDs are valid UUIDs.", finalPayment.getId(), e.getMessage());
            // Depending on policy, you might re-throw or just log this.
        }

        return finalPayment;
    }
}
