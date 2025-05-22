package br.com.hyteck.school_control.usecases.billing;

import br.com.hyteck.school_control.models.payments.Invoice;
import br.com.hyteck.school_control.models.payments.InvoiceStatus;
import br.com.hyteck.school_control.repositories.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;


import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class BillingScheduler {
    private final GenerateInvoicesForParents generateInvoicesForParents;
    private final InvoiceRepository invoiceRepository;
    private final ApplyPenaltyUseCase applyPenaltyUseCase;
    private final UpdateInvoiceStatusUseCase updateInvoiceStatusUseCase;
    private final PlatformTransactionManager transactionManager; // For manual transaction management if needed

    // Generate invoices on the 1st of every month at 1 AM
    @Scheduled(cron = "0 0 1 * * *") // "At 01:00 on day-of-month 1."
    public void generateMonthlyInvoicesScheduled() {
        YearMonth currentMonth = YearMonth.now(ZoneId.of("America/Sao_Paulo"));
        log.info("Scheduler: Starting monthly invoice generation for {}", currentMonth);
        try {
            // generateInvoicesForParents.execute is already @Transactional
            generateInvoicesForParents.execute(currentMonth);
            log.info("Scheduler: Monthly invoice generation completed for {}", currentMonth);
        } catch (Exception e) {
            log.error("Scheduler: Error during scheduled monthly invoice generation for {}: {}", currentMonth, e.getMessage(), e);
        }
    }

    // Check for overdue invoices, apply penalties, and update statuses daily at 2 AM
    @Scheduled(cron = "0 0 2 * * *") // "At 02:00 AM every day."
    public void processOverdueInvoicesAndStatusUpdates() {
        log.info("Scheduler: Starting daily check for overdue invoices and status updates.");
        LocalDate today = LocalDate.now(ZoneId.of("America/Sao_Paulo"));
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        // Find invoices that are PENDING or OVERDUE and past their due date
        // Process in pages to avoid loading too many invoices at once
        Pageable pageable = PageRequest.of(0, 100); // Process 100 invoices per batch
        Page<Invoice> invoicesToProcessPage;

        do {
            invoicesToProcessPage = invoiceRepository.findByStatusInAndDueDateBefore(
                    List.of(InvoiceStatus.PENDING, InvoiceStatus.OVERDUE),
                    today,
                    pageable
            );

            for (Invoice invoice : invoicesToProcessPage.getContent()) {
                try {
                    // Use manual transaction for each invoice to isolate failures
                    transactionTemplate.execute(new TransactionCallbackWithoutResult() {
                        @Override
                        protected void doInTransactionWithoutResult(TransactionStatus status) {
                            log.debug("Scheduler: Processing invoice ID {} with due date {} and status {}",
                                    invoice.getId(), invoice.getDueDate(), invoice.getStatus());

                            // Apply penalty if it's newly overdue (ApplyPenaltyUseCase handles eligibility checks)
                            // ApplyPenaltyUseCase is @Transactional itself
                            if (invoice.getStatus() == InvoiceStatus.PENDING && invoice.getDueDate().isBefore(today)) {
                                try {
                                    log.info("Scheduler: Invoice ID {} is PENDING and past due. Attempting to apply penalty.", invoice.getId());
                                    applyPenaltyUseCase.execute(invoice.getId());
                                } catch (Exception e) {
                                    log.error("Scheduler: Error applying penalty to Invoice ID {}: {}", invoice.getId(), e.getMessage(), e);
                                    // Decide if this error should rollback the current invoice processing
                                    // For now, log and continue to status update for this invoice
                                }
                            }

                            // Update the status of the invoice based on current balance and due date
                            // UpdateInvoiceStatusUseCase is @Transactional itself
                            try {
                                log.info("Scheduler: Updating status for Invoice ID {}", invoice.getId());
                                updateInvoiceStatusUseCase.execute(invoice.getId());
                            } catch (Exception e) {
                                log.error("Scheduler: Error updating status for Invoice ID {}: {}", invoice.getId(), e.getMessage(), e);
                            }
                        }
                    });
                } catch (Exception e) {
                    // Catch exceptions from transactionTemplate.execute itself, though typically it handles them
                    log.error("Scheduler: Critical error during processing of invoice ID {}: {}", invoice.getId(), e.getMessage(), e);
                }
            }
            pageable = invoicesToProcessPage.nextPageable();
        } while (invoicesToProcessPage.hasNext());

        log.info("Scheduler: Daily check for overdue invoices and status updates completed.");
    }
}
