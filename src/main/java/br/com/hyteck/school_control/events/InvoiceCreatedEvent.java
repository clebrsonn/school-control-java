package br.com.hyteck.school_control.events;

import br.com.hyteck.school_control.models.payments.Invoice;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class InvoiceCreatedEvent extends ApplicationEvent {

    private final Invoice invoice;

    /**
     * Create a new InvoiceCreatedEvent.
     *
     * @param source  the object on which the event initially occurred (never {@code null})
     * @param invoice the invoice that was created
     */
    public InvoiceCreatedEvent(Object source, Invoice invoice) {
        super(source);
        this.invoice = invoice;
    }
}
