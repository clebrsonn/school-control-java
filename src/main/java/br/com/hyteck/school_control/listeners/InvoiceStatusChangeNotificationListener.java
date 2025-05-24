package br.com.hyteck.school_control.listeners;

import br.com.hyteck.school_control.events.InvoiceStatusChangedEvent;
import br.com.hyteck.school_control.models.payments.Invoice;
import br.com.hyteck.school_control.models.payments.InvoiceStatus;
import br.com.hyteck.school_control.repositories.InvoiceRepository;
import br.com.hyteck.school_control.usecases.notification.CreateNotification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter; // Keep for specific month format
// Removed direct formatter imports
import br.com.hyteck.school_control.utils.FormatUtils; // Added

@Component
@RequiredArgsConstructor
@Slf4j
public class InvoiceStatusChangeNotificationListener {

    private final CreateNotification createNotificationUseCase;
    private final InvoiceRepository invoiceRepository; // To get invoice details

    // Formatters removed, will use FormatUtils

    @Async
    @EventListener
    public void handleInvoiceStatusChanged(final InvoiceStatusChangedEvent event) {
        log.info("Received InvoiceStatusChangedEvent for Invoice ID: {}. Old status: {}, New status: {}",
                event.getInvoiceId(), event.getOldStatus(), event.getNewStatus());

        if (event.getResponsibleUserId() == null) {
            log.warn("Responsible User ID is null in InvoiceStatusChangedEvent for Invoice ID: {}. Cannot send notification.", event.getInvoiceId());
            return;
        }

        final Invoice invoice = invoiceRepository.findById(event.getInvoiceId().toString()).orElse(null);
        if (invoice == null) {
            log.error("Invoice {} not found for status change notification.", event.getInvoiceId());
            return;
        }
        
        String invoiceIdentifier = "Fatura Ref. Mês " + invoice.getReferenceMonth().format(DateTimeFormatter.ofPattern("MM/yyyy"));
        if (invoice.getItems() != null && !invoice.getItems().isEmpty() &&
            invoice.getItems().getFirst().getEnrollment() != null && // defensive checks
            invoice.getItems().getFirst().getEnrollment().getStudent() != null) {
            final String studentName = invoice.getItems().getFirst().getEnrollment().getStudent().getName();
            invoiceIdentifier += " (Aluno: " + studentName + ")";
        }


        String message; // Not final as it's assigned in conditional blocks
        String notificationType; // Not final as it's assigned in conditional blocks

        if (event.getNewStatus() == InvoiceStatus.PAID) {
            message = String.format(
                    "O status da sua %s foi atualizado para PAGA. Obrigado!",
                    invoiceIdentifier
            );
            notificationType = "INVOICE_PAID";
        } else if (event.getNewStatus() == InvoiceStatus.OVERDUE) {
            message = String.format(
                    "Atenção: O status da sua %s foi atualizado para VENCIDA. Valor: %s, Vencimento: %s. Por favor, regularize.",
                    invoiceIdentifier,
                    FormatUtils.CURRENCY_FORMATTER.format(invoice.getAmount()), // Or current balance if available
                    invoice.getDueDate().format(FormatUtils.DATE_FORMATTER)
            );
            notificationType = "INVOICE_OVERDUE";
        } else if (event.getNewStatus() == InvoiceStatus.PENDING && event.getOldStatus() == InvoiceStatus.OVERDUE) {
            // This case might occur if a due date is extended or an error corrected.
             message = String.format(
                    "Informamos que o status da sua %s foi atualizado para PENDENTE. Novo vencimento: %s.",
                    invoiceIdentifier,
                    invoice.getDueDate().format(FormatUtils.DATE_FORMATTER)
            );
            notificationType = "INVOICE_PENDING_UPDATE";
        } else if (event.getNewStatus() == InvoiceStatus.PENDING && event.getOldStatus() == InvoiceStatus.PAID) {
            // This case might occur if a payment was reversed (e.g. chargeback)
             message = String.format(
                    "Atenção: O status da sua %s mudou de PAGA para PENDENTE. Valor: %s, Vencimento: %s. Verifique seus pagamentos ou entre em contato.",
                    invoiceIdentifier,
                    FormatUtils.CURRENCY_FORMATTER.format(invoice.getAmount()), // Or current balance
                    invoice.getDueDate().format(FormatUtils.DATE_FORMATTER)
            );
            notificationType = "INVOICE_PAYMENT_REVERSED"; // Example type
        } else {
            log.info("No specific notification configured for status change from {} to {} for invoice {}",
                    event.getOldStatus(), event.getNewStatus(), event.getInvoiceId());
            return; // Or a generic message
        }

        final String link = "/invoices/" + event.getInvoiceId();

        try {
            createNotificationUseCase.execute(
                    event.getResponsibleUserId().toString(),
                    message,
                    link,
                    notificationType
            );
            log.info("Notification sent for InvoiceStatusChangedEvent to User ID: {}, Invoice ID: {}",
                    event.getResponsibleUserId(), event.getInvoiceId());
        } catch (Exception e) {
            log.error("Error sending notification for InvoiceStatusChangedEvent to User ID {}: {}",
                    event.getResponsibleUserId(), e.getMessage(), e);
        }
    }
}
