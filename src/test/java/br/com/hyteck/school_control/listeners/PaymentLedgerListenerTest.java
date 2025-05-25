package br.com.hyteck.school_control.listeners;

import br.com.hyteck.school_control.events.PaymentProcessedEvent;
import br.com.hyteck.school_control.models.financial.Account;
import br.com.hyteck.school_control.models.financial.AccountType;
import br.com.hyteck.school_control.models.financial.LedgerEntryType;
import br.com.hyteck.school_control.models.payments.*;
import br.com.hyteck.school_control.repositories.InvoiceRepository;
import br.com.hyteck.school_control.repositories.PaymentRepository;
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
import org.slf4j.Logger; // Added import
import org.springframework.test.util.ReflectionTestUtils; // Added import


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class PaymentLedgerListenerTest {

    @Mock private LedgerService ledgerService;
    @Mock private AccountService accountService;
    @Mock private InvoiceRepository invoiceRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private ResponsibleRepository responsibleRepository;
    @Mock private LedgerEntryRepository ledgerEntryRepository; // Added mock

    @InjectMocks private PaymentLedgerListener listener;

    @Mock private Logger logger; // Added mock for SLF4J Logger

    @Captor private ArgumentCaptor<LocalDateTime> dateCaptor;
    @Captor private ArgumentCaptor<String> descriptionCaptor;


    private String paymentId;
    private String invoiceId;
    private String responsibleId;
    private BigDecimal amountPaid;
    private Payment payment;
    private Invoice invoice;
    private Responsible responsible;
    private Account cashAccount;
    private Account arAccount;
    private LocalDateTime paymentDate;

    @BeforeEach
    void setUp() {
        paymentId = UUID.randomUUID().toString();
        invoiceId = UUID.randomUUID().toString();
        responsibleId = UUID.randomUUID().toString();
        amountPaid = new BigDecimal("150.00");
        paymentDate = LocalDateTime.now().minusDays(1).withNano(0); // Remove nanos for easier comparison if needed

        payment = Payment.builder()
            .id(paymentId)
            .paymentDate(paymentDate)
            .paymentMethod(PaymentMethod.CREDIT_CARD)
            .method(PaymentMethod.CREDIT_CARD.name()) // Assuming string 'method' field as fallback
            .build();
            
        invoice = Invoice.builder().id(invoiceId).build();
        responsible = Responsible.builder().id(responsibleId).build();
        
        cashAccount = Account.builder().id("cashAccId").name("Cash/Bank Clearing").type(AccountType.ASSET).build();
        arAccount = Account.builder().id("arAccId").name("A/R Responsible").type(AccountType.ASSET).build();

        // Inject the mocked logger into the listener
        ReflectionTestUtils.setField(listener, "log", logger);
    }

    @Test
    void handlePaymentProcessedEvent_success_postsCorrectTransaction() {
        // Arrange
        PaymentProcessedEvent event = new PaymentProcessedEvent(this, paymentId, invoiceId, amountPaid, PaymentStatus.COMPLETED, InvoiceStatus.PAID, responsibleId);

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(responsibleRepository.findById(responsibleId)).thenReturn(Optional.of(responsible));
        when(accountService.findOrCreateAccount("Cash/Bank Clearing", AccountType.ASSET, null)).thenReturn(cashAccount);
        when(accountService.findOrCreateResponsibleARAccount(responsibleId)).thenReturn(arAccount);

        // Act
        listener.handlePaymentProcessedEvent(event);

        // Assert
        verify(ledgerService).postTransaction(
                eq(invoice),
                eq(payment),
                eq(cashAccount), // Debit
                eq(arAccount),   // Credit
                eq(amountPaid),
                eq(payment.getPaymentDate()),
                descriptionCaptor.capture(),
                eq(LedgerEntryType.PAYMENT_RECEIVED)
        );
        assertTrue(descriptionCaptor.getValue().contains("Payment " + paymentId + " received for Invoice #" + invoiceId));
        assertTrue(descriptionCaptor.getValue().contains(PaymentMethod.CREDIT_CARD.name()));
    }

    @Test
    void handlePaymentProcessedEvent_paymentDateIsNull_usesCurrentDateTime() {
        // Arrange
        payment.setPaymentDate(null); // Nullify payment date
        PaymentProcessedEvent event = new PaymentProcessedEvent(this, paymentId, invoiceId, amountPaid, PaymentStatus.COMPLETED, InvoiceStatus.PAID, responsibleId);

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(responsibleRepository.findById(responsibleId)).thenReturn(Optional.of(responsible));
        when(accountService.findOrCreateAccount("Cash/Bank Clearing", AccountType.ASSET, null)).thenReturn(cashAccount);
        when(accountService.findOrCreateResponsibleARAccount(responsibleId)).thenReturn(arAccount);

        // Act
        listener.handlePaymentProcessedEvent(event);

        // Assert
        verify(ledgerService).postTransaction(
                any(Invoice.class),
                any(Payment.class),
                any(Account.class),
                any(Account.class),
                any(BigDecimal.class),
                dateCaptor.capture(), // Capture the date
                anyString(),
                any(LedgerEntryType.class)
        );
        // Check that the captured date is very close to LocalDateTime.now()
        assertTrue(dateCaptor.getValue().isAfter(LocalDateTime.now().minusSeconds(5)));
        assertTrue(dateCaptor.getValue().isBefore(LocalDateTime.now().plusSeconds(5)));
    }
    
    @Test
    void handlePaymentProcessedEvent_whenPaymentNotFound_throwsExceptionAndDoesNotPost() {
        // Arrange
        PaymentProcessedEvent event = new PaymentProcessedEvent(this, paymentId, invoiceId, amountPaid, PaymentStatus.COMPLETED, InvoiceStatus.PAID, responsibleId);
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> listener.handlePaymentProcessedEvent(event));
        assertTrue(exception.getMessage().contains("Payment not found for ID: " + paymentId));
        verify(ledgerService, never()).postTransaction(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void handlePaymentProcessedEvent_whenInvoiceNotFound_throwsExceptionAndDoesNotPost() {
        // Arrange
        PaymentProcessedEvent event = new PaymentProcessedEvent(this, paymentId, invoiceId, amountPaid, PaymentStatus.COMPLETED, InvoiceStatus.PAID, responsibleId);
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment)); // Payment found
        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.empty()); // Invoice not found

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> listener.handlePaymentProcessedEvent(event));
        assertTrue(exception.getMessage().contains("Invoice not found for ID: " + invoiceId));
        verify(ledgerService, never()).postTransaction(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void handlePaymentProcessedEvent_whenResponsibleNotFound_throwsExceptionAndDoesNotPost() {
        // Arrange
        PaymentProcessedEvent event = new PaymentProcessedEvent(this, paymentId, invoiceId, amountPaid, PaymentStatus.COMPLETED, InvoiceStatus.PAID, responsibleId);
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(responsibleRepository.findById(responsibleId)).thenReturn(Optional.empty()); // Responsible not found

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> listener.handlePaymentProcessedEvent(event));
        assertTrue(exception.getMessage().contains("Responsible not found for ID: " + responsibleId));
        verify(ledgerService, never()).postTransaction(any(), any(), any(), any(), any(), any(), any(), any());
    }
    
    @Test
    void handlePaymentProcessedEvent_ledgerServiceFailure_rethrowsException() {
        // Arrange
        PaymentProcessedEvent event = new PaymentProcessedEvent(this, paymentId, invoiceId, amountPaid, PaymentStatus.COMPLETED, InvoiceStatus.PAID, responsibleId);

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(responsibleRepository.findById(responsibleId)).thenReturn(Optional.of(responsible));
        when(accountService.findOrCreateAccount("Cash/Bank Clearing", AccountType.ASSET, null)).thenReturn(cashAccount);
        when(accountService.findOrCreateResponsibleARAccount(responsibleId)).thenReturn(arAccount);
        
        doThrow(new RuntimeException("LedgerService failure")).when(ledgerService).postTransaction(
            any(Invoice.class), any(Payment.class), any(Account.class), any(Account.class), 
            any(BigDecimal.class), any(LocalDateTime.class), anyString(), any(LedgerEntryType.class)
        );

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> listener.handlePaymentProcessedEvent(event));
        assertEquals("LedgerService failure", exception.getMessage());
        
        // Verify postTransaction was indeed called before the exception
        verify(ledgerService).postTransaction(
                eq(invoice),
                eq(payment),
                eq(cashAccount),
                eq(arAccount),
                eq(amountPaid),
                eq(payment.getPaymentDate()),
                anyString(),
                eq(LedgerEntryType.PAYMENT_RECEIVED)
        );
    }

    // --- Tests for Idempotency ---

    @Test
    void handlePaymentProcessedEvent_whenEventNotProcessed_shouldPostTransactionAndLogSuccess() {
        // Arrange
        PaymentProcessedEvent event = new PaymentProcessedEvent(this, paymentId, invoiceId, amountPaid, PaymentStatus.COMPLETED, InvoiceStatus.PAID, responsibleId);
        when(ledgerEntryRepository.existsByPaymentIdAndType(paymentId, LedgerEntryType.PAYMENT_RECEIVED)).thenReturn(false);
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(responsibleRepository.findById(responsibleId)).thenReturn(Optional.of(responsible));
        when(accountService.findOrCreateAccount(anyString(), any(AccountType.class), isNull())).thenReturn(cashAccount);
        when(accountService.findOrCreateResponsibleARAccount(responsibleId)).thenReturn(arAccount);

        // Act
        listener.handlePaymentProcessedEvent(event);

        // Assert
        verify(ledgerService).postTransaction(
                eq(invoice),
                eq(payment),
                eq(cashAccount),
                eq(arAccount),
                eq(amountPaid),
                any(LocalDateTime.class), // Date might be set to now() if payment.getPaymentDate() is null
                anyString(),
                eq(LedgerEntryType.PAYMENT_RECEIVED)
        );
        verify(logger).info("PaymentLedgerListener: Successfully posted ledger entries for Payment ID: {}", paymentId);
    }

    @Test
    void handlePaymentProcessedEvent_whenEventAlreadyProcessed_shouldSkipAndLogInfo() {
        // Arrange
        PaymentProcessedEvent event = new PaymentProcessedEvent(this, paymentId, invoiceId, amountPaid, PaymentStatus.COMPLETED, InvoiceStatus.PAID, responsibleId);
        when(ledgerEntryRepository.existsByPaymentIdAndType(paymentId, LedgerEntryType.PAYMENT_RECEIVED)).thenReturn(true);

        // Act
        listener.handlePaymentProcessedEvent(event);

        // Assert
        verify(ledgerService, never()).postTransaction(any(), any(), any(), any(), any(), any(), any(), any());
        verify(logger).info("PaymentProcessedEvent for Payment ID: {} has already been processed. Skipping.", paymentId);
        verify(logger, never()).error(anyString(), anyString(), anyString(), anyString(), anyString(), any(Throwable.class)); // Ensure no error was logged
    }
}
