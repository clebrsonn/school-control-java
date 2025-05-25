package br.com.hyteck.school_control.usecases.billing;

import br.com.hyteck.school_control.exceptions.BusinessException;
import br.com.hyteck.school_control.exceptions.ResourceNotFoundException;
// import br.com.hyteck.school_control.models.financial.Account; // Removed
// import br.com.hyteck.school_control.models.financial.AccountType; // Removed
// import br.com.hyteck.school_control.models.financial.LedgerEntryType; // Removed
import br.com.hyteck.school_control.models.payments.Invoice;
import br.com.hyteck.school_control.models.payments.InvoiceStatus;
import br.com.hyteck.school_control.models.payments.Responsible;
import br.com.hyteck.school_control.repositories.InvoiceRepository;
import br.com.hyteck.school_control.events.PenaltyAssessedEvent;
// import br.com.hyteck.school_control.models.auth.User; // No longer needed if responsible setup changes
// import br.com.hyteck.school_control.services.financial.AccountService; // Removed
// import br.com.hyteck.school_control.services.financial.LedgerService; // Removed
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher; // Added
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor; // Added for event capturing
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
// import java.time.LocalDateTime; // Removed
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID; // Added for event payload verification

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplyPenaltyUseCaseTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    // @Mock private AccountService accountService; // Removed
    // @Mock private LedgerService ledgerService; // Removed
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ApplyPenaltyUseCase applyPenaltyUseCase;

    private Invoice overdueInvoice;
    private Invoice pendingInvoicePastDue;
    private Invoice pendingInvoiceNotDue;
    private Invoice paidInvoice;
    private Responsible responsible;
    // private Account arAccount; // Removed
    // private Account penaltyRevenueAccount; // Removed

    private final String invoiceId = "inv123";
    private final String responsibleId = "resp123";
    private static final BigDecimal PENALTY_AMOUNT = new BigDecimal("10.00");


    @BeforeEach
    void setUp() {
        responsible = Responsible.builder().id(responsibleId).name("Test Resp").username("respUserTest").build(); // username might not be needed
        // arAccount setup removed
        // penaltyRevenueAccount setup removed

        overdueInvoice = Invoice.builder()
                .id(invoiceId)
                .responsible(responsible)
                .status(InvoiceStatus.OVERDUE)
                .dueDate(LocalDate.now(ZoneId.of("America/Sao_Paulo")).minusDays(1))
                .amount(new BigDecimal("100"))
                .build();

        pendingInvoicePastDue = Invoice.builder()
                .id("invPendingPastDue")
                .responsible(responsible)
                .status(InvoiceStatus.PENDING)
                .dueDate(LocalDate.now(ZoneId.of("America/Sao_Paulo")).minusDays(1))
                .amount(new BigDecimal("200"))
                .build();
        
        pendingInvoiceNotDue = Invoice.builder()
                .id("invPendingNotDue")
                .responsible(responsible)
                .status(InvoiceStatus.PENDING)
                .dueDate(LocalDate.now(ZoneId.of("America/Sao_Paulo")).plusDays(5))
                .amount(new BigDecimal("300"))
                .build();

        paidInvoice = Invoice.builder()
                .id("invPaid")
                .responsible(responsible)
                .status(InvoiceStatus.PAID)
                .dueDate(LocalDate.now(ZoneId.of("America/Sao_Paulo")).minusDays(10))
                .amount(new BigDecimal("100"))
                .build();
    }

    private void setupMocksForSuccessfulPenaltyApplication() {
        // Removed accountService and ledgerService mocks from here
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));
        // eventPublisher mock is handled directly in tests or can be set here if always the same
    }


    @Test
    void execute_ShouldApplyPenalty_WhenInvoiceIsOverdue() {
        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(overdueInvoice));
        setupMocksForSuccessfulPenaltyApplication(); // Updated helper name

        assertDoesNotThrow(() -> applyPenaltyUseCase.execute(invoiceId));

        // verify(ledgerService).postTransaction(...); // Removed
        verify(invoiceRepository).save(overdueInvoice); 

        ArgumentCaptor<PenaltyAssessedEvent> eventCaptor = ArgumentCaptor.forClass(PenaltyAssessedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        PenaltyAssessedEvent publishedEvent = eventCaptor.getValue();
        assertEquals(UUID.fromString(invoiceId), publishedEvent.getInvoiceId());
        assertEquals(PENALTY_AMOUNT, publishedEvent.getPenaltyAmount());
        assertEquals(UUID.fromString(responsibleId), publishedEvent.getResponsibleId());
    }

    @Test
    void execute_ShouldApplyPenaltyAndUpdateStatus_WhenInvoiceIsPendingAndPastDue() {
        when(invoiceRepository.findById(pendingInvoicePastDue.getId())).thenReturn(Optional.of(pendingInvoicePastDue));
        setupMocksForSuccessfulPenaltyApplication(); // Updated helper name
        
        assertDoesNotThrow(() -> applyPenaltyUseCase.execute(pendingInvoicePastDue.getId()));

        assertEquals(InvoiceStatus.OVERDUE, pendingInvoicePastDue.getStatus());
        // verify(ledgerService).postTransaction(...); // Removed
        verify(invoiceRepository).save(pendingInvoicePastDue); 

        ArgumentCaptor<PenaltyAssessedEvent> eventCaptor = ArgumentCaptor.forClass(PenaltyAssessedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        PenaltyAssessedEvent publishedEvent = eventCaptor.getValue();
        assertEquals(UUID.fromString(pendingInvoicePastDue.getId()), publishedEvent.getInvoiceId());
        assertEquals(PENALTY_AMOUNT, publishedEvent.getPenaltyAmount());
        assertEquals(UUID.fromString(responsibleId), publishedEvent.getResponsibleId());
    }

    @Test
    void execute_ShouldThrowBusinessException_WhenInvoiceIsPendingAndNotDue() {
        when(invoiceRepository.findById(pendingInvoiceNotDue.getId())).thenReturn(Optional.of(pendingInvoiceNotDue));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> applyPenaltyUseCase.execute(pendingInvoiceNotDue.getId()));
        assertTrue(exception.getMessage().contains("is not eligible for penalty application"));
        // verifyNoInteractions(ledgerService); // ledgerService is removed
        verify(invoiceRepository, never()).save(any()); // Should not save if not eligible
        verifyNoInteractions(eventPublisher);
    }
    
    @Test
    void execute_ShouldThrowBusinessException_WhenInvoiceIsPaid() {
        when(invoiceRepository.findById(paidInvoice.getId())).thenReturn(Optional.of(paidInvoice));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> applyPenaltyUseCase.execute(paidInvoice.getId()));
        assertTrue(exception.getMessage().contains("is not eligible for penalty application"));
        // verifyNoInteractions(ledgerService); // ledgerService is removed
        verify(invoiceRepository, never()).save(any()); // Should not save if not eligible
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void execute_ShouldThrowResourceNotFoundException_WhenInvoiceNotFound() {
        String nonExistentId = "nonExistentId";
        when(invoiceRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> applyPenaltyUseCase.execute(nonExistentId));
        // verifyNoInteractions(accountService); // accountService is removed
        // verifyNoInteractions(ledgerService); // ledgerService is removed
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void execute_ShouldThrowBusinessException_WhenResponsibleOrUserIsNullOnInvoice() {
        // Test case 1: Responsible is null
        overdueInvoice.setResponsible(null); 
        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(overdueInvoice));
        BusinessException ex1 = assertThrows(BusinessException.class, () -> applyPenaltyUseCase.execute(invoiceId));
        assertEquals("Invoice responsible or user details not found. Cannot apply penalty.", ex1.getMessage());

        // Test case 2: Responsible's User is null
        overdueInvoice.setResponsible(Responsible.builder().id(responsibleId).build());
        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(overdueInvoice));
        BusinessException ex2 = assertThrows(BusinessException.class, () -> applyPenaltyUseCase.execute(invoiceId));
        assertEquals("Invoice responsible or user details not found. Cannot apply penalty.", ex2.getMessage());
        
        // verifyNoInteractions(ledgerService); // ledgerService is removed
        verify(invoiceRepository, never()).save(any()); // Should not save if responsible is invalid
        verifyNoInteractions(eventPublisher);
    }
}
