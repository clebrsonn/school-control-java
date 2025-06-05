package br.com.hyteck.school_control.services;

// Removed ResourceNotFoundException, Invoice, InvoiceItem, BigDecimal, Optional related imports
import br.com.hyteck.school_control.repositories.InvoiceRepository;
// Removed BeforeEach, Test
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// Removed static imports for Assertions and Mockito
@ExtendWith(MockitoExtension.class)
public class InvoiceServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @InjectMocks
    private InvoiceService invoiceService;

    // Removed mockInvoiceItem, mockInvoice, invoiceItemId, invoiceId fields
    // Removed setUp() method

    // All test methods related to updateInvoiceItemAmount have been removed:
    // - shouldUpdateInvoiceItemAmountSuccessfully
    // - shouldThrowResourceNotFoundExceptionWhenInvoiceItemNotFound
    // - shouldThrowIllegalStateExceptionWhenInvoiceItemHasNoParentInvoice
    // - shouldCallUpdateAmountOnInvoiceItem

    // This class is now empty of tests. It can be kept as a placeholder
    // for future InvoiceService-specific tests or deleted if deemed unnecessary.
}
