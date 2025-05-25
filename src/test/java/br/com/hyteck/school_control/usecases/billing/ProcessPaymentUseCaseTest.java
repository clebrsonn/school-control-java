package br.com.hyteck.school_control.usecases.billing;

import br.com.hyteck.school_control.exceptions.BusinessException;
import br.com.hyteck.school_control.exceptions.ResourceNotFoundException;
import br.com.hyteck.school_control.models.financial.Account;
import br.com.hyteck.school_control.models.financial.AccountType; // Still needed for arAccount setup
// import br.com.hyteck.school_control.models.financial.LedgerEntryType; // Removed
import br.com.hyteck.school_control.models.payments.*;
import br.com.hyteck.school_control.repositories.InvoiceRepository;
import br.com.hyteck.school_control.repositories.PaymentRepository;
import br.com.hyteck.school_control.repositories.financial.LedgerEntryRepository;
import br.com.hyteck.school_control.events.PaymentProcessedEvent;
import br.com.hyteck.school_control.services.financial.AccountService;
// import br.com.hyteck.school_control.services.financial.LedgerService; // Removed
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher; // Added
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcessPaymentUseCaseTest {

    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private AccountService accountService;
    // @Mock private LedgerService ledgerService; // Removed
    @Mock
    private LedgerEntryRepository ledgerEntryRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher; // Added

    @InjectMocks
    private ProcessPaymentUseCase processPaymentUseCase;

    private Invoice pendingInvoice;
    private Invoice overdueInvoice;
    private Responsible responsible;
    private Account arAccount;
    // private Account cashClearingAccount; // Removed as it's no longer used
    private final String invoiceId = "inv1";
    private final String responsibleId = "resp1";

    @BeforeEach
    void setUp() {

        responsible = Responsible.builder().id(responsibleId).name("Test Responsible").username("respUser").build();
        arAccount = Account.builder().id("arAcc1").type(AccountType.ASSET).responsible(responsible).name("A/R - Test Responsible").build();
        // cashClearingAccount = Account.builder().id("cashAcc1").type(AccountType.ASSET).name("Cash/Bank Clearing").build(); // Removed

        pendingInvoice = Invoice.builder()
                .id(invoiceId)
                .responsible(responsible)
                .status(InvoiceStatus.PENDING)
                .dueDate(LocalDate.now(ZoneId.of("America/Sao_Paulo")).plusDays(5))
                .amount(new BigDecimal("200.00"))
                .build();
        
        overdueInvoice = Invoice.builder()
                .id("invOverdue1")
                .responsible(responsible)
                .status(InvoiceStatus.OVERDUE)
                .dueDate(LocalDate.now(ZoneId.of("America/Sao_Paulo")).minusDays(5))
                .amount(new BigDecimal("150.00"))
                .build();
    }

    private void mockSuccessfulPaymentScenario(Invoice invoiceToProcess, BigDecimal paymentAmount, BigDecimal resultingBalanceOnAR) {

        when(invoiceRepository.findById(invoiceToProcess.getId())).thenReturn(Optional.of(invoiceToProcess));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            p.setId("payment123"); 
            return p;
        });
        // Use the actual responsibleId from the invoiceToProcess for consistency
        when(accountService.findOrCreateResponsibleARAccount(invoiceToProcess.getResponsible().getId())).thenReturn(arAccount);
        // Removed: when(accountService.findOrCreateAccount("Cash/Bank Clearing", AccountType.ASSET, null)).thenReturn(cashClearingAccount);
        // Removed: doNothing().when(ledgerService).postTransaction(...);
        
        // This 'resultingBalanceOnAR' is the balance *before* the current payment is applied by the listener.
        // The use case will subtract the current payment amount from this to determine if the invoice is paid.
        when(ledgerEntryRepository.getBalanceForInvoiceOnAccount(arAccount.getId(), invoiceToProcess.getId())).thenReturn(resultingBalanceOnAR);
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void execute_ShouldProcessFullPayment_AndMarkInvoiceAsPaid() {
        // Arrange
        BigDecimal paymentAmount = new BigDecimal("200.00");
        // To make effectiveBalanceForStatusCheck = 0, mock getBalanceForInvoiceOnAccount to return current paymentAmount
        mockSuccessfulPaymentScenario(pendingInvoice, paymentAmount, paymentAmount); 

        // Act
        Payment resultPayment = processPaymentUseCase.execute(invoiceId, paymentAmount, PaymentMethod.CREDIT_CARD);

        // Assert
        assertNotNull(resultPayment);
        assertEquals(PaymentStatus.COMPLETED, resultPayment.getStatus());
        assertEquals(InvoiceStatus.PAID, pendingInvoice.getStatus()); // Verify invoice status changed

        // verify(ledgerService).postTransaction(...); // Removed
        verify(invoiceRepository).save(pendingInvoice);
        verify(paymentRepository, times(2)).save(resultPayment); 
        verify(eventPublisher).publishEvent(any(PaymentProcessedEvent.class)); // Verify event publication
    }
    
    @Test
    void execute_ShouldProcessFullPaymentForOverdueInvoice_AndMarkInvoiceAsPaid() {
        BigDecimal paymentAmount = new BigDecimal("150.00");
        // To make effectiveBalanceForStatusCheck = 0, mock getBalanceForInvoiceOnAccount to return current paymentAmount
        mockSuccessfulPaymentScenario(overdueInvoice, paymentAmount, paymentAmount);

        Payment resultPayment = processPaymentUseCase.execute(overdueInvoice.getId(), paymentAmount, PaymentMethod.BANK_TRANSFER);

        assertEquals(PaymentStatus.COMPLETED, resultPayment.getStatus());
        assertEquals(InvoiceStatus.PAID, overdueInvoice.getStatus());
        verify(invoiceRepository).save(overdueInvoice);
        verify(eventPublisher).publishEvent(any(PaymentProcessedEvent.class));
    }


    @Test
    void execute_ShouldProcessPartialPayment_AndKeepInvoicePending() {
        // Arrange
        BigDecimal paymentAmount = new BigDecimal("100.00");
        // To make effectiveBalanceForStatusCheck > 0 (e.g., 100), mock getBalanceForInvoiceOnAccount to return current paymentAmount + 100
        // Original invoice amount is 200. Payment is 100. Remaining should be 100.
        // So, balance *before* this payment was 200.
        mockSuccessfulPaymentScenario(pendingInvoice, paymentAmount, new BigDecimal("200.00"));

        // Act
        Payment resultPayment = processPaymentUseCase.execute(invoiceId, paymentAmount, PaymentMethod.DEBIT_CARD);

        // Assert
        assertNotNull(resultPayment);
        assertEquals(PaymentStatus.COMPLETED, resultPayment.getStatus());
        assertEquals(InvoiceStatus.PENDING, pendingInvoice.getStatus()); // Still PENDING as not fully paid and not overdue

        // verify(ledgerService).postTransaction(...); // Removed
        verify(invoiceRepository).save(pendingInvoice);
        verify(eventPublisher).publishEvent(any(PaymentProcessedEvent.class));
    }
    
    @Test
    void execute_ShouldProcessPartialPayment_AndMarkInvoiceOverdue_IfPastDueDate() {
        BigDecimal paymentAmount = new BigDecimal("50.00");
        // Original overdue invoice amount is 150. Payment is 50. Remaining should be 100.
        // So, balance *before* this payment was 150.
        mockSuccessfulPaymentScenario(overdueInvoice, paymentAmount, new BigDecimal("150.00"));

        Payment resultPayment = processPaymentUseCase.execute(overdueInvoice.getId(), paymentAmount, PaymentMethod.PIX);

        assertEquals(PaymentStatus.COMPLETED, resultPayment.getStatus());
        assertEquals(InvoiceStatus.OVERDUE, overdueInvoice.getStatus()); // Remains OVERDUE
        verify(invoiceRepository).save(overdueInvoice);
        verify(eventPublisher).publishEvent(any(PaymentProcessedEvent.class));
    }


    @Test
    void execute_ShouldThrowResourceNotFoundException_WhenInvoiceNotFound() {
        String nonExistentInvoiceId = "invNotFound";
        when(invoiceRepository.findById(nonExistentInvoiceId)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () ->
                processPaymentUseCase.execute(nonExistentInvoiceId, new BigDecimal("100"), PaymentMethod.CREDIT_CARD));
        assertEquals("Invoice not found with ID: " + nonExistentInvoiceId, ex.getMessage());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void execute_ShouldThrowBusinessException_WhenInvoiceAlreadyPaid() {
        pendingInvoice.setStatus(InvoiceStatus.PAID);
        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(pendingInvoice));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                processPaymentUseCase.execute(invoiceId, new BigDecimal("100"), PaymentMethod.CREDIT_CARD));
        assertEquals("Invoice is already paid.", ex.getMessage());
        verifyNoInteractions(eventPublisher);
    }
    
    @Test
    void execute_ShouldThrowBusinessException_WhenInvoiceCancelled() {
        pendingInvoice.setStatus(InvoiceStatus.CANCELLED);
        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(pendingInvoice));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                processPaymentUseCase.execute(invoiceId, new BigDecimal("100"), PaymentMethod.CREDIT_CARD));
        assertEquals("Invoice is already cancelled.", ex.getMessage());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void execute_ShouldThrowBusinessException_WhenResponsibleOrUserIsNullOnInvoice() {
        // Test case 1: Responsible is null
        pendingInvoice.setResponsible(null);
        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(pendingInvoice));
        BusinessException ex1 = assertThrows(BusinessException.class, () ->
                processPaymentUseCase.execute(invoiceId, new BigDecimal("100"), PaymentMethod.CREDIT_CARD));
        assertEquals("Invoice responsible party or user details not found. Cannot process payment or publish event.", ex1.getMessage());

        // Test case 2: Responsible's User is null
        pendingInvoice.setResponsible(Responsible.builder().id(responsibleId).build()); // User is null
        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(pendingInvoice));
        BusinessException ex2 = assertThrows(BusinessException.class, () ->
            processPaymentUseCase.execute(invoiceId, new BigDecimal("100"), PaymentMethod.CREDIT_CARD));
        assertEquals("Invoice responsible party or user details not found. Cannot process payment or publish event.", ex2.getMessage());
        
        // Test case 3: Responsible's User ID is null
        pendingInvoice.setResponsible(Responsible.builder().id(responsibleId).build()); // User ID is null
        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(pendingInvoice));
        BusinessException ex3 = assertThrows(BusinessException.class, () ->
            processPaymentUseCase.execute(invoiceId, new BigDecimal("100"), PaymentMethod.CREDIT_CARD));
        assertEquals("Invoice responsible party or user details not found. Cannot process payment or publish event.", ex3.getMessage());
        
        verifyNoInteractions(eventPublisher);
    }
}
