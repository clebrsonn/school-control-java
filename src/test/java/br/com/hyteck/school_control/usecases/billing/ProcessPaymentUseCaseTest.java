package br.com.hyteck.school_control.usecases.billing;

import br.com.hyteck.school_control.exceptions.BusinessException;
import br.com.hyteck.school_control.exceptions.ResourceNotFoundException;
import br.com.hyteck.school_control.models.financial.Account;
import br.com.hyteck.school_control.models.financial.AccountType;
import br.com.hyteck.school_control.models.financial.LedgerEntryType;
import br.com.hyteck.school_control.models.payments.*;
import br.com.hyteck.school_control.repositories.InvoiceRepository;
import br.com.hyteck.school_control.repositories.PaymentRepository;
import br.com.hyteck.school_control.repositories.financial.LedgerEntryRepository;
import br.com.hyteck.school_control.events.PaymentProcessedEvent;
import br.com.hyteck.school_control.models.auth.User; // Needed for responsible.user
import br.com.hyteck.school_control.services.financial.AccountService;
import br.com.hyteck.school_control.services.financial.LedgerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher; // Added
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
    @Mock
    private LedgerService ledgerService;
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
    private Account cashClearingAccount;
    private String invoiceId = "inv1";
    private String responsibleId = "resp1";

    @BeforeEach
    void setUp() {
        User responsibleUser = User.builder().id("userResp1").username("respUser").build(); // User for responsible
        responsible = Responsible.builder().id(responsibleId).name("Test Responsible").user(responsibleUser).build();
        arAccount = Account.builder().id("arAcc1").type(AccountType.ASSET).responsible(responsible).name("A/R - Test Responsible").build();
        cashClearingAccount = Account.builder().id("cashAcc1").type(AccountType.ASSET).name("Cash/Bank Clearing").build();

        pendingInvoice = Invoice.builder()
                .id(invoiceId)
                .responsible(responsible)
                .status(InvoiceStatus.PENDING)
                .dueDate(LocalDate.now(ZoneId.of("America/Sao_Paulo")).plusDays(5))
                .originalAmount(new BigDecimal("200.00"))
                .build();
        
        overdueInvoice = Invoice.builder()
                .id("invOverdue1")
                .responsible(responsible)
                .status(InvoiceStatus.OVERDUE)
                .dueDate(LocalDate.now(ZoneId.of("America/Sao_Paulo")).minusDays(5))
                .originalAmount(new BigDecimal("150.00"))
                .build();
    }

    private void mockSuccessfulPaymentScenario(Invoice invoiceToProcess, BigDecimal paymentAmount, BigDecimal resultingBalanceOnAR) {
        // Ensure the invoice used in the test has a responsible with a user and user ID.
        if (invoiceToProcess.getResponsible() != null && invoiceToProcess.getResponsible().getUser() == null) {
            User mockUser = User.builder().id("defaultUserIdForTest").build();
            invoiceToProcess.getResponsible().setUser(mockUser);
        } else if (invoiceToProcess.getResponsible() == null) {
            User mockUser = User.builder().id("defaultUserIdForTest").build();
            Responsible mockResp = Responsible.builder().id("defaultRespIdForTest").user(mockUser).build();
            invoiceToProcess.setResponsible(mockResp);
        }


        when(invoiceRepository.findById(invoiceToProcess.getId())).thenReturn(Optional.of(invoiceToProcess));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            p.setId("payment123"); 
            return p;
        });
        // Use the actual responsibleId from the invoiceToProcess for consistency
        when(accountService.findOrCreateResponsibleARAccount(invoiceToProcess.getResponsible().getId())).thenReturn(arAccount);
        when(accountService.findOrCreateAccount("Cash/Bank Clearing", AccountType.ASSET, null)).thenReturn(cashClearingAccount);
        doNothing().when(ledgerService).postTransaction(
                any(Invoice.class), any(Payment.class), any(Account.class), any(Account.class),
                any(BigDecimal.class), any(LocalDateTime.class), anyString(), any(LedgerEntryType.class)
        );
        when(ledgerEntryRepository.getBalanceForInvoiceOnAccount(arAccount.getId(), invoiceToProcess.getId())).thenReturn(resultingBalanceOnAR);
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void execute_ShouldProcessFullPayment_AndMarkInvoiceAsPaid() {
        // Arrange
        BigDecimal paymentAmount = new BigDecimal("200.00");
        // After payment, balance on A/R for this invoice becomes 0
        mockSuccessfulPaymentScenario(pendingInvoice, paymentAmount, BigDecimal.ZERO);

        // Act
        Payment resultPayment = processPaymentUseCase.execute(invoiceId, paymentAmount, PaymentMethod.CREDIT_CARD);

        // Assert
        assertNotNull(resultPayment);
        assertEquals(PaymentStatus.COMPLETED, resultPayment.getStatus());
        assertEquals(InvoiceStatus.PAID, pendingInvoice.getStatus()); // Verify invoice status changed

        verify(ledgerService).postTransaction(
                eq(pendingInvoice), eq(resultPayment), eq(cashClearingAccount), eq(arAccount),
                eq(paymentAmount), any(LocalDateTime.class),
                contains("Payment received for Invoice #" + invoiceId), eq(LedgerEntryType.PAYMENT_RECEIVED)
        );
        verify(invoiceRepository).save(pendingInvoice);
        verify(paymentRepository, times(2)).save(resultPayment); 
        verify(eventPublisher).publishEvent(any(PaymentProcessedEvent.class)); // Verify event publication
    }
    
    @Test
    void execute_ShouldProcessFullPaymentForOverdueInvoice_AndMarkInvoiceAsPaid() {
        BigDecimal paymentAmount = new BigDecimal("150.00");
        mockSuccessfulPaymentScenario(overdueInvoice, paymentAmount, BigDecimal.ZERO);

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
        // After partial payment, balance on A/R for this invoice is 100.00 (200 original - 100 paid)
        mockSuccessfulPaymentScenario(pendingInvoice, paymentAmount, new BigDecimal("100.00"));

        // Act
        Payment resultPayment = processPaymentUseCase.execute(invoiceId, paymentAmount, PaymentMethod.DEBIT_CARD);

        // Assert
        assertNotNull(resultPayment);
        assertEquals(PaymentStatus.COMPLETED, resultPayment.getStatus());
        assertEquals(InvoiceStatus.PENDING, pendingInvoice.getStatus()); // Still PENDING as not fully paid and not overdue

        verify(ledgerService).postTransaction(
            eq(pendingInvoice), eq(resultPayment), eq(cashClearingAccount), eq(arAccount),
            eq(paymentAmount), any(LocalDateTime.class),
            contains("Payment received for Invoice #" + invoiceId), eq(LedgerEntryType.PAYMENT_RECEIVED)
        );
        verify(invoiceRepository).save(pendingInvoice);
        verify(eventPublisher).publishEvent(any(PaymentProcessedEvent.class));
    }
    
    @Test
    void execute_ShouldProcessPartialPayment_AndMarkInvoiceOverdue_IfPastDueDate() {
        BigDecimal paymentAmount = new BigDecimal("50.00");
        // After partial payment, balance on A/R for this invoice is 100.00 (150 original - 50 paid)
        mockSuccessfulPaymentScenario(overdueInvoice, paymentAmount, new BigDecimal("100.00"));

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
        pendingInvoice.setResponsible(Responsible.builder().id(responsibleId).user(null).build()); // User is null
        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(pendingInvoice));
        BusinessException ex2 = assertThrows(BusinessException.class, () ->
            processPaymentUseCase.execute(invoiceId, new BigDecimal("100"), PaymentMethod.CREDIT_CARD));
        assertEquals("Invoice responsible party or user details not found. Cannot process payment or publish event.", ex2.getMessage());
        
        // Test case 3: Responsible's User ID is null
        pendingInvoice.setResponsible(Responsible.builder().id(responsibleId).user(User.builder().id(null).build()).build()); // User ID is null
        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(pendingInvoice));
        BusinessException ex3 = assertThrows(BusinessException.class, () ->
            processPaymentUseCase.execute(invoiceId, new BigDecimal("100"), PaymentMethod.CREDIT_CARD));
        assertEquals("Invoice responsible party or user details not found. Cannot process payment or publish event.", ex3.getMessage());
        
        verifyNoInteractions(eventPublisher);
    }
}
