package br.com.hyteck.school_control.listeners;

import br.com.hyteck.school_control.events.PenaltyAssessedEvent;
import br.com.hyteck.school_control.models.payments.Invoice;
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
public class PenaltyNotificationListener {

    private final CreateNotification createNotificationUseCase;
    private final InvoiceRepository invoiceRepository; // To get more context if needed

    // Formatters removed, will use FormatUtils

    @Async
    @EventListener
    public void handlePenaltyAssessed(final PenaltyAssessedEvent event) {
        log.info("Received PenaltyAssessedEvent for Invoice ID: {}, Amount: {}",
                event.getInvoiceId(), event.getPenaltyAmount());

        if (event.getResponsibleUserId() == null) {
            log.warn("Responsible User ID is null in PenaltyAssessedEvent for Invoice ID: {}. Cannot send notification.", event.getInvoiceId());
            return;
        }

        final Invoice invoice = invoiceRepository.findById(event.getInvoiceId().toString()).orElse(null);
        if (invoice == null) {
            log.error("Invoice {} not found for penalty notification.", event.getInvoiceId());
            return;
        }
        
        String invoiceIdentifier = "Fatura Ref. Mês " + invoice.getReferenceMonth().format(DateTimeFormatter.ofPattern("MM/yyyy"));
         if (invoice.getItems() != null && !invoice.getItems().isEmpty() && 
            invoice.getItems().getFirst().getEnrollment() != null && 
            invoice.getItems().getFirst().getEnrollment().getStudent() != null) {
            final String studentName = invoice.getItems().getFirst().getEnrollment().getStudent().getName();
            invoiceIdentifier += " (Aluno: " + studentName + ")";
        }


        final String formattedPenaltyAmount = FormatUtils.CURRENCY_FORMATTER.format(event.getPenaltyAmount());
        final String message = String.format(
                "Uma multa de %s foi aplicada à sua %s devido a atraso no pagamento. O valor atualizado pode ser consultado nos detalhes da fatura.",
                formattedPenaltyAmount,
                invoiceIdentifier
        );
        final String notificationType = "INVOICE_PENALTY_ASSESSED";
        final String link = "/invoices/" + event.getInvoiceId();

        try {
            createNotificationUseCase.execute(
                    event.getResponsibleUserId().toString(),
                    message,
                    link,
                    notificationType
            );
            log.info("Notification sent for PenaltyAssessedEvent to User ID: {}, Invoice ID: {}",
                    event.getResponsibleUserId(), event.getInvoiceId());
        } catch (Exception e) {
            log.error("Error sending notification for PenaltyAssessedEvent to User ID {}: {}",
                    event.getResponsibleUserId(), e.getMessage(), e);
        }
    }
}
