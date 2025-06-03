package br.com.hyteck.school_control.listeners;

import br.com.hyteck.school_control.events.PenaltyAssessedEvent;
import br.com.hyteck.school_control.models.financial.Account;
import br.com.hyteck.school_control.models.financial.AccountType;
import br.com.hyteck.school_control.models.financial.LedgerEntryType;
import br.com.hyteck.school_control.models.payments.Invoice;
import br.com.hyteck.school_control.models.payments.Responsible;
import br.com.hyteck.school_control.repositories.InvoiceRepository;
import br.com.hyteck.school_control.repositories.ResponsibleRepository;
import br.com.hyteck.school_control.services.financial.AccountService;
import br.com.hyteck.school_control.services.financial.LedgerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class PenaltyLedgerListenerTest {

    @Mock private LedgerService ledgerService;
    @Mock private AccountService accountService;
    @Mock private InvoiceRepository invoiceRepository;
    @Mock private ResponsibleRepository responsibleRepository;

    @InjectMocks private PenaltyLedgerListener listener;

    @Captor private ArgumentCaptor<LocalDateTime> dateCaptor;
    @Captor private ArgumentCaptor<String> descriptionCaptor;

    private UUID invoiceIdUuid;
    private UUID responsibleIdUuid;
    private BigDecimal penaltyAmount;
    private Invoice invoice;
    private Responsible responsible;
    private Account arAccount;
    private Account penaltyRevenueAccount;

    @BeforeEach
    void setUp() {
        invoiceIdUuid = UUID.randomUUID();
        responsibleIdUuid = UUID.randomUUID();
        penaltyAmount = new BigDecimal("25.00");

        invoice = Invoice.builder().id(invoiceIdUuid.toString()).build();
        responsible = Responsible.builder().id(responsibleIdUuid.toString()).build();
        
        arAccount = Account.builder().id("arAccId").name("A/R Responsible").type(AccountType.ASSET).build();
        penaltyRevenueAccount = Account.builder().id("penaltyRevAccId").name("Penalty Revenue").type(AccountType.REVENUE).build();
    }

    @Test
    void handlePenaltyAssessedEvent_success_postsCorrectTransaction() {
        // Arrange
        PenaltyAssessedEvent event = new PenaltyAssessedEvent(this, invoiceIdUuid, penaltyAmount, responsibleIdUuid);

        when(invoiceRepository.findById(invoiceIdUuid.toString())).thenReturn(Optional.of(invoice));
        when(responsibleRepository.findById(responsibleIdUuid.toString())).thenReturn(Optional.of(responsible));
        when(accountService.findOrCreateResponsibleARAccount(responsible.getId())).thenReturn(arAccount);
        when(accountService.findOrCreateAccount("Penalty Revenue", AccountType.REVENUE, null)).thenReturn(penaltyRevenueAccount);

        // Act
        listener.handlePenaltyAssessedEvent(event);

        // Assert
        verify(ledgerService).postTransaction(
                eq(invoice),
                isNull(),        // No payment object
                eq(arAccount),   // Debit A/R
                eq(penaltyRevenueAccount), // Credit Penalty Revenue
                eq(penaltyAmount),
                dateCaptor.capture(),
                descriptionCaptor.capture(),
                eq(LedgerEntryType.PENALTY_ASSESSED)
        );
        
        assertTrue(dateCaptor.getValue().isBefore(LocalDateTime.now().plusSeconds(2)) && dateCaptor.getValue().isAfter(LocalDateTime.now().minusSeconds(5)));
        assertEquals("Penalty assessed for overdue Invoice #" + invoice.getId(), descriptionCaptor.getValue());
    }

    @Test
    void handlePenaltyAssessedEvent_whenInvoiceNotFound_throwsExceptionAndDoesNotPost() {
        // Arrange
        PenaltyAssessedEvent event = new PenaltyAssessedEvent(this, invoiceIdUuid, penaltyAmount, responsibleIdUuid);
        when(invoiceRepository.findById(invoiceIdUuid.toString())).thenReturn(Optional.empty());
        // responsibleRepository.findById will not be called if invoice is not found first.

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> listener.handlePenaltyAssessedEvent(event));
        assertTrue(exception.getMessage().contains("Invoice not found for ID: " + invoiceIdUuid.toString()));
        verify(ledgerService, never()).postTransaction(any(), any(), any(), any(), any(), any(), any(), any());
    }
    
    @Test
    void handlePenaltyAssessedEvent_whenResponsibleNotFound_throwsExceptionAndDoesNotPost() {
        // Arrange
        PenaltyAssessedEvent event = new PenaltyAssessedEvent(this, invoiceIdUuid, penaltyAmount, responsibleIdUuid);
        when(invoiceRepository.findById(invoiceIdUuid.toString())).thenReturn(Optional.of(invoice)); // Invoice found
        when(responsibleRepository.findById(responsibleIdUuid.toString())).thenReturn(Optional.empty()); // Responsible not found

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> listener.handlePenaltyAssessedEvent(event));
        assertTrue(exception.getMessage().contains("Responsible not found for ID: " + responsibleIdUuid.toString()));
        verify(ledgerService, never()).postTransaction(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void handlePenaltyAssessedEvent_ledgerServiceFailure_rethrowsException() {
        // Arrange
        PenaltyAssessedEvent event = new PenaltyAssessedEvent(this, invoiceIdUuid, penaltyAmount, responsibleIdUuid);

        when(invoiceRepository.findById(invoiceIdUuid.toString())).thenReturn(Optional.of(invoice));
        when(responsibleRepository.findById(responsibleIdUuid.toString())).thenReturn(Optional.of(responsible));
        when(accountService.findOrCreateResponsibleARAccount(responsible.getId())).thenReturn(arAccount);
        when(accountService.findOrCreateAccount("Penalty Revenue", AccountType.REVENUE, null)).thenReturn(penaltyRevenueAccount);
        
        doThrow(new RuntimeException("LedgerService test failure")).when(ledgerService).postTransaction(
            any(Invoice.class), isNull(), any(Account.class), any(Account.class), 
            any(BigDecimal.class), any(LocalDateTime.class), anyString(), any(LedgerEntryType.class)
        );

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> listener.handlePenaltyAssessedEvent(event));
        assertEquals("LedgerService test failure", exception.getMessage());
        
        // Verify postTransaction was indeed called
        verify(ledgerService).postTransaction(
                eq(invoice),
                isNull(),
                eq(arAccount),
                eq(penaltyRevenueAccount),
                eq(penaltyAmount),
                any(LocalDateTime.class), // Date is captured but not strictly checked here as it's covered in success test
                anyString(), // Description is captured but not strictly checked here
                eq(LedgerEntryType.PENALTY_ASSESSED)
        );
    }
}
