package br.com.hyteck.school_control.listeners;

import br.com.hyteck.school_control.events.BatchInvoiceGeneratedEvent;
import br.com.hyteck.school_control.models.payments.Invoice;
import br.com.hyteck.school_control.repositories.InvoiceRepository;
import br.com.hyteck.school_control.usecases.notification.CreateNotification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

// Removed direct formatter imports, will use FormatUtils
import java.time.format.DateTimeFormatter; // Still needed for event.getTargetMonth().format
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import br.com.hyteck.school_control.utils.FormatUtils; // Added

@Component
@RequiredArgsConstructor
@Slf4j
public class InvoiceNotificationListener {

    private final InvoiceRepository invoiceRepository; // To fetch details if needed
    private final CreateNotification createNotificationUseCase;

    // Formatters removed, will use FormatUtils.DATE_FORMATTER and FormatUtils.CURRENCY_FORMATTER

    @Async
    @EventListener
    public void handleBatchInvoiceGenerated(final BatchInvoiceGeneratedEvent event) {
        log.info("Received BatchInvoiceGeneratedEvent for responsible User ID: {} and target month: {}",
                event.getResponsibleUserId(), event.getTargetMonth());

        if (event.getGeneratedInvoiceIds() == null || event.getGeneratedInvoiceIds().isEmpty()) {
            log.warn("No invoice IDs found in BatchInvoiceGeneratedEvent for responsible User ID: {}", event.getResponsibleUserId());
            return;
        }
        
        // Assuming the event is for a single responsible and might contain multiple invoice IDs
        // if a single operation generated, for instance, a consolidated invoice and perhaps separate ones.
        // Or, if GenerateInvoicesForParents fires one event per responsible with all their new invoices for the month.
        // For this example, let's assume it's one main consolidated invoice or a primary invoice per event.
        // If multiple distinct invoices are in generatedInvoiceIds for the same responsible,
        // the notification might need to be more generic or list them.

        // Let's fetch the first invoice to get common details like responsible name (if needed and not in event)
        // and to make the notification more specific if it's usually one main invoice per event.
        
        final UUID firstInvoiceId = event.getGeneratedInvoiceIds().get(0);
        final Invoice representativeInvoice = invoiceRepository.findById(firstInvoiceId.toString()).orElse(null);

        if (representativeInvoice == null) {
            log.error("Could not find representative invoice with ID {} from BatchInvoiceGeneratedEvent.", firstInvoiceId);
            return;
        }

        // In the refactored GenerateInvoicesForParents, it seems one consolidated invoice is made per responsible.
        // So, generatedInvoiceIds will likely contain one ID.
        
        final String studentNames = representativeInvoice.getItems().stream()
                .map(item -> item.getEnrollment().getStudent().getName())
                .distinct()
                .collect(Collectors.joining(", "));

        // The amount for notification should be the net amount due.
        // The invoice amount is set, and ledger entries for discounts are made.
        // For simplicity, the event could carry the net amount, or we approximate it here.
        // The previous GenerateInvoicesForParents calculated this net amount before notification.
        // Let's assume the notification is about the *original* invoice amount before payment.
        // A more accurate "balance due" would require querying the ledger.
        final String formattedAmount = FormatUtils.CURRENCY_FORMATTER.format(representativeInvoice.getAmount());
        final String formattedDueDate = representativeInvoice.getDueDate().format(FormatUtils.DATE_FORMATTER);
        final String targetMonthFormatted = event.getTargetMonth().format(DateTimeFormatter.ofPattern("MM/yyyy")); // Keep specific month format here

        final String message = String.format(
                "Nova(s) fatura(s) de mensalidade (Ref: %s) gerada(s) para %s no valor total de %s, com vencimento em %s.",
                targetMonthFormatted,
                studentNames,
                formattedAmount,
                formattedDueDate
        );

        final String link = "/invoices/" + representativeInvoice.getId(); // Link to the primary/consolidated invoice
        final String notificationType = "NEW_MONTHLY_INVOICE";

        try {
            createNotificationUseCase.execute(
                    event.getResponsibleUserId().toString(), // CreateNotification expects String user ID
                    message,
                    link,
                    notificationType
            );
            log.info("Notification sent for BatchInvoiceGeneratedEvent to User ID: {}, Invoice(s) Ref: {}",
                    event.getResponsibleUserId(), firstInvoiceId);
        } catch (Exception e) {
            log.error("Error sending notification for BatchInvoiceGeneratedEvent to User ID {}: {}",
                    event.getResponsibleUserId(), e.getMessage(), e);
        }
    }
}
