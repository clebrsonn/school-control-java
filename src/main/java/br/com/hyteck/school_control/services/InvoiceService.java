package br.com.hyteck.school_control.services;

import br.com.hyteck.school_control.exceptions.ResourceNotFoundException;
import br.com.hyteck.school_control.models.payments.Invoice;
import br.com.hyteck.school_control.models.payments.InvoiceItem;
import br.com.hyteck.school_control.repositories.InvoiceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;

    @Autowired
    public InvoiceService(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    @Transactional
    public InvoiceItem updateInvoiceItemAmount(String invoiceItemId, BigDecimal newAmount) {
        InvoiceItem invoiceItem = invoiceRepository.findInvoiceItemById(invoiceItemId);

        if (invoiceItem == null) {
            throw new ResourceNotFoundException("InvoiceItem not found with id: " + invoiceItemId);
        }

        // Get the parent Invoice before updating the item
        Invoice invoice = invoiceItem.getInvoice();
        if (invoice == null) {
            // This case should ideally not happen if data integrity is maintained
            // (i.e., an InvoiceItem should always be associated with an Invoice).
            // If it can happen, specific error handling or logging might be needed.
            // For now, we'll assume an InvoiceItem always has a parent Invoice.
            // If not, saving just the invoiceItem might be an option, but then
            // the Invoice total recalculation logic via @PreUpdate on Invoice won't trigger.
            throw new IllegalStateException("InvoiceItem with id " + invoiceItemId + " is not associated with an Invoice.");
        }

        invoiceItem.updateAmount(newAmount); // This updates the amount on the item

        // Save the parent Invoice to trigger @PreUpdate and recalculate totals
        // CascadeType.ALL on Invoice.items should ensure InvoiceItem changes are persisted too.
        invoiceRepository.save(invoice);

        // The invoiceItem instance here is part of the 'invoice' object's collection.
        // After 'invoiceRepository.save(invoice)', the 'invoiceItem' within that collection
        // will reflect the updated amount.
        // We need to return the updated item. It should be the same instance.
        return invoiceItem;
    }
}
