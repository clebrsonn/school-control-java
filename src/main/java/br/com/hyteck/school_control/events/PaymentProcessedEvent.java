package br.com.hyteck.school_control.events;

import br.com.hyteck.school_control.models.payments.InvoiceStatus;
import br.com.hyteck.school_control.models.payments.PaymentStatus;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.math.BigDecimal;
// Assuming String IDs
// import java.util.UUID;

/**
 * Event published when a payment has been successfully processed.
 */
@Getter
public class PaymentProcessedEvent extends ApplicationEvent {

    private final String paymentId;
    private final String invoiceId;
    private final BigDecimal amountPaid;
    private final PaymentStatus paymentStatus; // e.g., COMPLETED, FAILED
    private final InvoiceStatus updatedInvoiceStatus; // e.g., PAID, PENDING, OVERDUE
    private final String responsibleUserId; // For targeted notification

    /**
     * Constructs a new PaymentProcessedEvent.
     *
     * @param source               The component that published the event.
     * @param paymentId            The ID of the processed payment.
     * @param invoiceId            The ID of the invoice associated with the payment.
     * @param amountPaid           The amount that was paid.
     * @param paymentStatus        The final status of the payment.
     * @param updatedInvoiceStatus The updated status of the invoice after the payment.
     * @param responsibleUserId    The User ID of the responsible party for the invoice.
     */
    public PaymentProcessedEvent(Object source, String paymentId, String invoiceId, BigDecimal amountPaid,
                                 PaymentStatus paymentStatus, InvoiceStatus updatedInvoiceStatus, String responsibleUserId) {
        super(source);
        this.paymentId = paymentId;
        this.invoiceId = invoiceId;
        this.amountPaid = amountPaid;
        this.paymentStatus = paymentStatus;
        this.updatedInvoiceStatus = updatedInvoiceStatus;
        this.responsibleUserId = responsibleUserId;
    }
}
