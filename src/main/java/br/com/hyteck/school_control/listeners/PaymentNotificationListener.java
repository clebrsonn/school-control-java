package br.com.hyteck.school_control.listeners;

import br.com.hyteck.school_control.events.PaymentProcessedEvent;
import br.com.hyteck.school_control.models.payments.Invoice;
import br.com.hyteck.school_control.repositories.InvoiceRepository;
import br.com.hyteck.school_control.usecases.notification.CreateNotification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

// Removed direct formatter imports
import java.util.UUID;
import br.com.hyteck.school_control.utils.FormatUtils; // Added


@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentNotificationListener {

    private final CreateNotification createNotificationUseCase;
    private final InvoiceRepository invoiceRepository; // To get more context if needed

    // CURRENCY_FORMATTER removed, will use FormatUtils.CURRENCY_FORMATTER

    @Async
    @EventListener
    public void handlePaymentProcessed(final PaymentProcessedEvent event) {
        log.info("Received PaymentProcessedEvent for Payment ID: {}, Invoice ID: {}",
                event.getPaymentId(), event.getInvoiceId());

        if (event.getResponsibleUserId() == null) {
            log.warn("Responsible User ID is null in PaymentProcessedEvent for Payment ID: {}. Cannot send notification.", event.getPaymentId());
            return;
        }

        // Optional: Fetch invoice for more details, e.g., student names for a richer message
        final Invoice invoice = invoiceRepository.findById(event.getInvoiceId().toString()).orElse(null);
        String forItemDescription; // Not final as it's conditionally assigned
        if (invoice != null && invoice.getResponsible() != null) {
            forItemDescription = " para " + invoice.getResponsible().getName(); // Or student names from items
        } else {
            log.warn("Could not retrieve full invoice details for PaymentProcessedEvent - Invoice ID: {}", event.getInvoiceId());
            forItemDescription = " (Fatura ID: " + event.getInvoiceId().toString().substring(0,8) + "...)";
        }


        final String formattedAmount = FormatUtils.CURRENCY_FORMATTER.format(event.getAmountPaid());
        String message; // Not final as it's assigned in switch
        String notificationType; // Not final as it's assigned in switch

        switch (event.getPaymentStatus()) {
            case COMPLETED:
                message = String.format(
                        "Seu pagamento de %s referente à fatura %s foi processado com sucesso. Status da fatura: %s.",
                        formattedAmount,
                        forItemDescription,
                        event.getUpdatedInvoiceStatus().getFriendlyName() // Assuming InvoiceStatus has a getFriendlyName() or similar
                );
                notificationType = "PAYMENT_SUCCESS";
                break;
            case FAILED:
                message = String.format(
                        "Houve uma falha ao processar seu pagamento de %s para a fatura %s. Por favor, tente novamente ou entre em contato.",
                        formattedAmount,
                        forItemDescription
                );
                notificationType = "PAYMENT_FAILURE";
                break;
            case PENDING_CONFIRMATION:
                 message = String.format(
                        "Seu pagamento de %s para a fatura %s está pendente de confirmação. Avisaremos assim que for concluído.",
                        formattedAmount,
                        forItemDescription
                );
                notificationType = "PAYMENT_PENDING";
                break;
            default:
                log.warn("Unhandled payment status {} in PaymentProcessedEvent for Payment ID: {}", event.getPaymentStatus(), event.getPaymentId());
                return;
        }

        final String link = "/invoices/" + event.getInvoiceId(); // Link to the invoice

        try {
            createNotificationUseCase.execute(
                    event.getResponsibleUserId().toString(), // CreateNotification expects String user ID
                    message,
                    link,
                    notificationType
            );
            log.info("Notification sent for PaymentProcessedEvent to User ID: {}, Payment ID: {}",
                    event.getResponsibleUserId(), event.getPaymentId());
        } catch (Exception e) {
            log.error("Error sending notification for PaymentProcessedEvent to User ID {}: {}",
                    event.getResponsibleUserId(), e.getMessage(), e);
        }
    }
}
