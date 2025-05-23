package br.com.hyteck.school_control.events;

import br.com.hyteck.school_control.models.payments.InvoiceStatus;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.UUID;

/**
 * Event published when the status of an invoice changes.
 */
@Getter
public class InvoiceStatusChangedEvent extends ApplicationEvent {

    private final UUID invoiceId;
    private final InvoiceStatus oldStatus;
    private final InvoiceStatus newStatus;
    private final UUID responsibleUserId; // User ID of the responsible party for notification

    /**
     * Constructs a new InvoiceStatusChangedEvent.
     *
     * @param source            The component that published the event.
     * @param invoiceId         The ID of the invoice whose status changed.
     * @param oldStatus         The previous status of the invoice.
     * @param newStatus         The new (current) status of the invoice.
     * @param responsibleUserId The User ID of the responsible party associated with the invoice.
     */
    public InvoiceStatusChangedEvent(Object source, UUID invoiceId, InvoiceStatus oldStatus,
                                     InvoiceStatus newStatus, UUID responsibleUserId) {
        super(source);
        this.invoiceId = invoiceId;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.responsibleUserId = responsibleUserId;
    }
}
