package br.com.hyteck.school_control.events;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

/**
 * Event published when a batch of invoices has been generated for a responsible party.
 */
@Getter
public class BatchInvoiceGeneratedEvent extends ApplicationEvent {

    private final List<UUID> generatedInvoiceIds; // List of UUIDs for the generated invoices
    private final YearMonth targetMonth;
    private final UUID responsibleId; // UUID of the responsible party
    private final UUID responsibleUserId; // User UUID of the responsible party for notification

    /**
     * Constructs a new BatchInvoiceGeneratedEvent.
     *
     * @param source              The component that published the event (or any object).
     * @param generatedInvoiceIds A list of UUIDs of the invoices that were generated.
     * @param targetMonth         The month for which the invoices were generated.
     * @param responsibleId       The UUID of the responsible party for whom the invoices were generated.
     * @param responsibleUserId   The User UUID of the responsible party, for notification purposes.
     */
    public BatchInvoiceGeneratedEvent(Object source, List<UUID> generatedInvoiceIds, YearMonth targetMonth, UUID responsibleId, UUID responsibleUserId) {
        super(source);
        // Ensure the list is immutable
        this.generatedInvoiceIds = (generatedInvoiceIds == null) ? Collections.emptyList() : List.copyOf(generatedInvoiceIds);
        this.targetMonth = targetMonth;
        this.responsibleId = responsibleId;
        this.responsibleUserId = responsibleUserId;
    }
}
