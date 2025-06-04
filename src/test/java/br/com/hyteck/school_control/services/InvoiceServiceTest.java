package br.com.hyteck.school_control.services;

import br.com.hyteck.school_control.exceptions.ResourceNotFoundException;
import br.com.hyteck.school_control.models.payments.Invoice;
import br.com.hyteck.school_control.models.payments.InvoiceItem;
import br.com.hyteck.school_control.repositories.InvoiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class InvoiceServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @InjectMocks
    private InvoiceService invoiceService;

    private InvoiceItem mockInvoiceItem;
    private Invoice mockInvoice;
    private final String invoiceItemId = "item-123";
    private final String invoiceId = "invoice-abc";

    @BeforeEach
    void setUp() {
        mockInvoice = new Invoice();
        mockInvoice.setId(invoiceId);
        // mockInvoice.setItems(new ArrayList<>()); // Initialize if needed, though not directly used in these tests for adding items

        mockInvoiceItem = spy(new InvoiceItem()); // Spy to allow partial mocking / verifying calls on real methods if needed
        mockInvoiceItem.setId(invoiceItemId);
        mockInvoiceItem.setInvoice(mockInvoice); // Associate item with invoice
        // mockInvoice.getItems().add(mockInvoiceItem); // Add item to invoice's list
    }

    @Test
    void shouldUpdateInvoiceItemAmountSuccessfully() {
        BigDecimal newAmount = new BigDecimal("200.00");
        when(invoiceRepository.findInvoiceItemById(invoiceItemId)).thenReturn(mockInvoiceItem);
        // The save method on repository returns the saved entity.
        // For Invoice, it returns the Invoice instance.
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(mockInvoice);


        InvoiceItem updatedItem = invoiceService.updateInvoiceItemAmount(invoiceItemId, newAmount);

        assertNotNull(updatedItem);
        // verify that updateAmount on the real (spied) invoiceItem was called
        verify(mockInvoiceItem).updateAmount(newAmount);
        // verify that invoiceRepository.save was called on the parent invoice
        verify(invoiceRepository).save(mockInvoice);
        assertEquals(newAmount, updatedItem.getAmount()); // Check if amount was updated in the returned item
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenInvoiceItemNotFound() {
        BigDecimal newAmount = new BigDecimal("200.00");
        when(invoiceRepository.findInvoiceItemById(invoiceItemId)).thenReturn(null); // or Optional.empty() if repo returned Optional

        Exception exception = assertThrows(ResourceNotFoundException.class, () -> {
            invoiceService.updateInvoiceItemAmount(invoiceItemId, newAmount);
        });

        assertEquals("InvoiceItem not found with id: " + invoiceItemId, exception.getMessage());
        verify(invoiceRepository, never()).save(any(Invoice.class));
    }

    @Test
    void shouldThrowIllegalStateExceptionWhenInvoiceItemHasNoParentInvoice() {
        BigDecimal newAmount = new BigDecimal("200.00");
        mockInvoiceItem.setInvoice(null); // Detach from parent invoice
        when(invoiceRepository.findInvoiceItemById(invoiceItemId)).thenReturn(mockInvoiceItem);

        Exception exception = assertThrows(IllegalStateException.class, () -> {
            invoiceService.updateInvoiceItemAmount(invoiceItemId, newAmount);
        });

        assertEquals("InvoiceItem with id " + invoiceItemId + " is not associated with an Invoice.", exception.getMessage());
        verify(mockInvoiceItem, never()).updateAmount(any(BigDecimal.class)); // updateAmount should not be called
        verify(invoiceRepository, never()).save(any(Invoice.class)); // save should not be called
    }


    @Test
    void shouldCallUpdateAmountOnInvoiceItem() {
        BigDecimal newAmount = new BigDecimal("250.00");
        when(invoiceRepository.findInvoiceItemById(invoiceItemId)).thenReturn(mockInvoiceItem);
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(mockInvoice);


        invoiceService.updateInvoiceItemAmount(invoiceItemId, newAmount);

        // Verifies that mockInvoiceItem.updateAmount was called with the specific newAmount
        verify(mockInvoiceItem).updateAmount(newAmount);
    }
}
