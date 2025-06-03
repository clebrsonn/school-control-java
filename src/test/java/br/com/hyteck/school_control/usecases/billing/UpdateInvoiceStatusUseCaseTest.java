package br.com.hyteck.school_control.usecases.billing;

import br.com.hyteck.school_control.exceptions.BusinessException;
import br.com.hyteck.school_control.exceptions.ResourceNotFoundException;
import br.com.hyteck.school_control.models.financial.Account;
import br.com.hyteck.school_control.models.financial.AccountType;
import br.com.hyteck.school_control.models.payments.Invoice;
import br.com.hyteck.school_control.models.payments.InvoiceStatus;
import br.com.hyteck.school_control.models.payments.Responsible;
import br.com.hyteck.school_control.repositories.InvoiceRepository;
import br.com.hyteck.school_control.repositories.financial.LedgerEntryRepository;
import br.com.hyteck.school_control.events.InvoiceStatusChangedEvent;
import br.com.hyteck.school_control.models.auth.User; // Needed for responsible.user
import br.com.hyteck.school_control.services.financial.AccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.ApplicationEventPublisher; // Added
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UpdateInvoiceStatusUseCaseTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private AccountService accountService;

    @Mock
    private LedgerEntryRepository ledgerEntryRepository;

    // ApplyPenaltyUseCase is no longer directly called by UpdateInvoiceStatusUseCase in the refined version.
    // private ApplyPenaltyUseCase applyPenaltyUseCase;
    @Mock
    private ApplicationEventPublisher eventPublisher; // Added

    @InjectMocks
    private UpdateInvoiceStatusUseCase updateInvoiceStatusUseCase;

    private Invoice testInvoice;
    private Responsible testResponsible;
    private Account testArAccount;
    private String invoiceId = "invTest123";
    private String responsibleId = "respTest123";
    private String arAccountId = "arAccTest123";

    @BeforeEach
    void setUp() {
        User responsibleUser = User.builder().id("userRespTest1").build();
        testResponsible = Responsible.builder().id(responsibleId).name("Test Responsible").username("respUserTest").build();
        testArAccount = Account.builder().id(arAccountId).type(AccountType.ASSET).responsible(testResponsible).build();

        testInvoice = Invoice.builder()
                .id(invoiceId)
                .responsible(testResponsible)
                .status(InvoiceStatus.PENDING) // Default starting status for many tests
                .dueDate(LocalDate.now(ZoneId.of("America/Sao_Paulo")).plusDays(10)) // Due in future
                .amount(new BigDecimal("100.00"))
                .build();
        
        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(testInvoice));
        when(accountService.findOrCreateResponsibleARAccount(responsibleId)).thenReturn(testArAccount);
    }

    @Test
    void execute_ShouldUpdateStatusToPaid_WhenBalanceIsZero() {
        when(ledgerEntryRepository.getBalanceForInvoiceOnAccount(arAccountId, invoiceId)).thenReturn(BigDecimal.ZERO);

        Invoice updatedInvoice = updateInvoiceStatusUseCase.execute(invoiceId);

        assertEquals(InvoiceStatus.PAID, updatedInvoice.getStatus());
        verify(invoiceRepository).save(testInvoice);
        verify(eventPublisher).publishEvent(any(InvoiceStatusChangedEvent.class));
    }

    @Test
    void execute_ShouldUpdateStatusToPaid_WhenBalanceIsNegative() {
        when(ledgerEntryRepository.getBalanceForInvoiceOnAccount(arAccountId, invoiceId)).thenReturn(new BigDecimal("-10.00")); // Overpaid

        Invoice updatedInvoice = updateInvoiceStatusUseCase.execute(invoiceId);

        assertEquals(InvoiceStatus.PAID, updatedInvoice.getStatus());
        verify(invoiceRepository).save(testInvoice);
        verify(eventPublisher).publishEvent(any(InvoiceStatusChangedEvent.class));
    }

    @Test
    void execute_ShouldUpdateStatusToOverdue_WhenBalanceIsPositiveAndDueDateInPast() {
        testInvoice.setDueDate(LocalDate.now(ZoneId.of("America/Sao_Paulo")).minusDays(1)); // Due yesterday
        when(ledgerEntryRepository.getBalanceForInvoiceOnAccount(arAccountId, invoiceId)).thenReturn(new BigDecimal("50.00"));

        Invoice updatedInvoice = updateInvoiceStatusUseCase.execute(invoiceId);

        assertEquals(InvoiceStatus.OVERDUE, updatedInvoice.getStatus());
        verify(invoiceRepository).save(testInvoice);
        verify(eventPublisher).publishEvent(any(InvoiceStatusChangedEvent.class));
    }

    @Test
    void execute_ShouldKeepStatusPending_WhenBalanceIsPositiveAndDueDateInFuture() {
        testInvoice.setStatus(InvoiceStatus.PENDING); // Explicitly set for clarity
        when(ledgerEntryRepository.getBalanceForInvoiceOnAccount(arAccountId, invoiceId)).thenReturn(new BigDecimal("50.00"));

        Invoice updatedInvoice = updateInvoiceStatusUseCase.execute(invoiceId);

        assertEquals(InvoiceStatus.PENDING, updatedInvoice.getStatus());
        // Status doesn't change from PENDING to PENDING, so save might not be called if no actual change
        // However, the current implementation saves if oldStatus != newStatus. If they are same, it won't.
        verify(invoiceRepository, never()).save(testInvoice);
        verifyNoInteractions(eventPublisher); // No status change, no event
    }
    
    @Test
    void execute_ShouldChangeFromOverdueToPending_WhenDueDateIsFutureAndBalancePositive() {
        testInvoice.setStatus(InvoiceStatus.OVERDUE);
        testInvoice.setDueDate(LocalDate.now(ZoneId.of("America/Sao_Paulo")).plusDays(1)); // Due date now in future
        when(ledgerEntryRepository.getBalanceForInvoiceOnAccount(arAccountId, invoiceId)).thenReturn(new BigDecimal("50.00"));

        Invoice updatedInvoice = updateInvoiceStatusUseCase.execute(invoiceId);

        assertEquals(InvoiceStatus.PENDING, updatedInvoice.getStatus());
        verify(invoiceRepository).save(testInvoice);
        verify(eventPublisher).publishEvent(any(InvoiceStatusChangedEvent.class));
    }


    @Test
    void execute_ShouldNotUpdateStatus_IfCalculatedStatusIsSameAsCurrent() {
        testInvoice.setStatus(InvoiceStatus.OVERDUE);
        testInvoice.setDueDate(LocalDate.now(ZoneId.of("America/Sao_Paulo")).minusDays(1));
        when(ledgerEntryRepository.getBalanceForInvoiceOnAccount(arAccountId, invoiceId)).thenReturn(new BigDecimal("50.00")); // Stays OVERDUE

        Invoice updatedInvoice = updateInvoiceStatusUseCase.execute(invoiceId);

        assertEquals(InvoiceStatus.OVERDUE, updatedInvoice.getStatus());
        verify(invoiceRepository, never()).save(testInvoice); // No change, no save
        verifyNoInteractions(eventPublisher); // No status change, no event
    }
    
    @Test
    void execute_ShouldReturnInvoiceAsIs_WhenStatusIsCancelled() {
        testInvoice.setStatus(InvoiceStatus.CANCELLED);
        
        Invoice result = updateInvoiceStatusUseCase.execute(invoiceId);

        assertEquals(InvoiceStatus.CANCELLED, result.getStatus());
        verify(invoiceRepository, never()).save(any(Invoice.class)); 
        verifyNoInteractions(ledgerEntryRepository); 
        verifyNoInteractions(accountService); 
        verifyNoInteractions(eventPublisher);
    }


    @Test
    void execute_ShouldThrowResourceNotFoundException_WhenInvoiceNotFound() {
        when(invoiceRepository.findById("unknownId")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> updateInvoiceStatusUseCase.execute("unknownId"));
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void execute_ShouldThrowResourceNotFoundException_WhenResponsibleOrUserIsNullOnInvoice() {
        // Test Case 1: Responsible is null
        testInvoice.setResponsible(null); 
        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(testInvoice)); 
        ResourceNotFoundException ex1 = assertThrows(ResourceNotFoundException.class, () -> updateInvoiceStatusUseCase.execute(invoiceId));
        assertTrue(ex1.getMessage().contains("Responsible party or user details not found"));
        
        // Reset responsible and make its user null
        testInvoice.setResponsible(Responsible.builder().id(responsibleId).build());
        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(testInvoice));
        ResourceNotFoundException ex2 = assertThrows(ResourceNotFoundException.class, () -> updateInvoiceStatusUseCase.execute(invoiceId));
        assertTrue(ex2.getMessage().contains("Responsible party or user details not found"));
        verifyNoInteractions(eventPublisher);
    }
    
    @Test
    void execute_ShouldReEvaluateStatus_WhenInvoiceWasPaidButBalanceIsNowPositive() {
        testInvoice.setStatus(InvoiceStatus.PAID);
        testInvoice.setDueDate(LocalDate.now(ZoneId.of("America/Sao_Paulo")).minusDays(5)); // Due in past
        // Simulate that it was paid, but now due to a refund (not shown here), its balance is positive
        when(ledgerEntryRepository.getBalanceForInvoiceOnAccount(arAccountId, invoiceId)).thenReturn(new BigDecimal("20.00"));

        Invoice updatedInvoice = updateInvoiceStatusUseCase.execute(invoiceId);

        // Since balance is positive and due date is past, it should become OVERDUE
        assertEquals(InvoiceStatus.OVERDUE, updatedInvoice.getStatus());
        verify(invoiceRepository).save(testInvoice);
        verify(eventPublisher).publishEvent(any(InvoiceStatusChangedEvent.class));
    }

    @Test
    void execute_ShouldKeepStatusPaid_WhenInvoiceIsPaidAndBalanceRemainsZeroOrLess() {
        testInvoice.setStatus(InvoiceStatus.PAID);
        testInvoice.setDueDate(LocalDate.now(ZoneId.of("America/Sao_Paulo")).minusDays(5));
        when(ledgerEntryRepository.getBalanceForInvoiceOnAccount(arAccountId, invoiceId)).thenReturn(BigDecimal.ZERO);

        Invoice updatedInvoice = updateInvoiceStatusUseCase.execute(invoiceId);
        
        assertEquals(InvoiceStatus.PAID, updatedInvoice.getStatus());
        // If status was already PAID and remains PAID, no save should occur.
        verify(invoiceRepository, never()).save(testInvoice);
        verifyNoInteractions(eventPublisher); // No status change, no event
    }
}
