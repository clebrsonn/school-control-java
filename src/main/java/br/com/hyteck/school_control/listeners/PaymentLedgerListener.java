package br.com.hyteck.school_control.listeners;

import br.com.hyteck.school_control.events.PaymentProcessedEvent;
import br.com.hyteck.school_control.models.financial.Account;
import br.com.hyteck.school_control.models.financial.AccountType;
import br.com.hyteck.school_control.models.financial.LedgerEntryType;
import br.com.hyteck.school_control.models.payments.Invoice;
import br.com.hyteck.school_control.models.payments.Payment;
import br.com.hyteck.school_control.models.payments.Responsible;
import br.com.hyteck.school_control.repositories.InvoiceRepository;
import br.com.hyteck.school_control.repositories.PaymentRepository;
import br.com.hyteck.school_control.repositories.ResponsibleRepository;
import br.com.hyteck.school_control.repositories.financial.LedgerEntryRepository;
import br.com.hyteck.school_control.services.financial.AccountService;
import br.com.hyteck.school_control.services.financial.LedgerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
// Consider adding specific exception imports if needed, e.g., for ResourceNotFoundException
// import br.com.hyteck.school_control.exceptions.ResourceNotFoundException; 

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentLedgerListener {

    private final LedgerService ledgerService;
    private final AccountService accountService;
    // Repositories are included as per the example, assuming event carries IDs
    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final ResponsibleRepository responsibleRepository;
    private final LedgerEntryRepository ledgerEntryRepository;


    @EventListener
    @Transactional
    public void handlePaymentProcessedEvent(PaymentProcessedEvent event) {
        log.info("PaymentLedgerListener: Received PaymentProcessedEvent for Payment ID: {}, Invoice ID: {}, Amount: {}",
                event.getPaymentId(), event.getInvoiceId(), event.getAmountPaid());

        try {
            // Idempotency check
            if (ledgerEntryRepository.existsByPaymentIdAndType(event.getPaymentId(), LedgerEntryType.PAYMENT_RECEIVED)) {
                log.info("PaymentProcessedEvent for Payment ID: {} has already been processed. Skipping.", event.getPaymentId());
                return;
            }

            // Fetch entities based on IDs from the event
            Payment payment = paymentRepository.findById(event.getPaymentId())
                    .orElseThrow(() -> new RuntimeException("Payment not found for ID: " + event.getPaymentId())); // Consider specific exception
            
            Invoice invoice = invoiceRepository.findById(event.getInvoiceId())
                    .orElseThrow(() -> new RuntimeException("Invoice not found for ID: " + event.getInvoiceId())); // Consider specific exception

            // Assuming responsibleId is directly on the event. If not, might need to get from invoice.getResponsible().getId()
            // For this implementation, sticking to the event structure assumption.
            Responsible responsible = responsibleRepository.findById(event.getResponsibleId()) 
                    .orElseThrow(() -> new RuntimeException("Responsible not found for ID: " + event.getResponsibleId())); // Consider specific exception
            
            // Determine accounts
            Account cashOrBankClearingAccount = accountService.findOrCreateAccount("Cash/Bank Clearing", AccountType.ASSET, null);
            Account responsibleARAccount = accountService.findOrCreateResponsibleARAccount(responsible.getId());

            // Prepare description
            String paymentMethodName = "N/A";
            if (payment.getPaymentMethod() != null) {
                paymentMethodName = payment.getPaymentMethod().name(); // Assuming PaymentMethod is an enum
            } else if (payment.getMethod() != null && !payment.getMethod().isBlank()){
                 paymentMethodName = payment.getMethod(); // Assuming there's a string field 'method' as fallback
            }

            String description = String.format("Payment %s received for Invoice #%s via %s",
                                               payment.getId(), 
                                               invoice.getId(), 
                                               paymentMethodName);
            
            // Determine transaction date
            LocalDateTime transactionDate = payment.getPaymentDate() != null ? payment.getPaymentDate() : LocalDateTime.now();

            // Post transaction
            ledgerService.postTransaction(
                    invoice,
                    payment,
                    cashOrBankClearingAccount,  // Debit account
                    responsibleARAccount,       // Credit account
                    event.getAmountPaid(),      // Amount
                    transactionDate,            // Transaction date
                    description,                // Description
                    LedgerEntryType.PAYMENT_RECEIVED // Ledger entry type
            );
            log.info("PaymentLedgerListener: Successfully posted ledger entries for Payment ID: {}", event.getPaymentId());

        } catch (Exception e) {
            // Log the error with event details for traceability
            log.error("PaymentLedgerListener: Failed to process PaymentProcessedEvent for Payment ID: {}. Error: {} " + 
                      "- Invoice ID: {}, Responsible ID: {}, Amount: {}",
                    event.getPaymentId(), e.getMessage(), event.getInvoiceId(), event.getResponsibleId(), event.getAmountPaid(), e);
            // Re-throwing the exception ensures the transaction is rolled back
            // and allows for further handling by the Spring event publishing mechanism if configured.
            throw e; 
        }
    }
}
