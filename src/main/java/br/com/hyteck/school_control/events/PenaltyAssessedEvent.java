package br.com.hyteck.school_control.events;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Event published when a penalty has been assessed on an invoice.
 */
@Getter
public class PenaltyAssessedEvent extends ApplicationEvent {

    private final UUID invoiceId;
    private final BigDecimal penaltyAmount;
    private final UUID responsibleUserId; // User ID of the responsible party for notification

    /**
     * Constructs a new PenaltyAssessedEvent.
     *
     * @param source            The component that published the event.
     * @param invoiceId         The ID of the invoice on which the penalty was assessed.
     * @param penaltyAmount     The amount of the penalty.
     * @param responsibleUserId The User ID of the responsible party associated with the invoice.
     */
    public PenaltyAssessedEvent(Object source, UUID invoiceId, BigDecimal penaltyAmount, UUID responsibleUserId) {
        super(source);
        this.invoiceId = invoiceId;
        this.penaltyAmount = penaltyAmount;
        this.responsibleUserId = responsibleUserId;
    }
}
