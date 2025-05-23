package br.com.hyteck.school_control.usecases.billing;

import br.com.hyteck.school_control.exceptions.BusinessException;
import br.com.hyteck.school_control.exceptions.ResourceNotFoundException;
import br.com.hyteck.school_control.models.financial.Account;
import br.com.hyteck.school_control.models.financial.AccountType;
import br.com.hyteck.school_control.models.financial.LedgerEntryType;
import br.com.hyteck.school_control.models.payments.Invoice;
import br.com.hyteck.school_control.models.payments.InvoiceStatus;
import br.com.hyteck.school_control.models.payments.Responsible;
import br.com.hyteck.school_control.repositories.InvoiceRepository;
import br.com.hyteck.school_control.events.PenaltyAssessedEvent;
import br.com.hyteck.school_control.models.auth.User; // Needed for responsible.user
import br.com.hyteck.school_control.services.financial.AccountService;
import br.com.hyteck.school_control.services.financial.LedgerService;
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
class ApplyPenaltyUseCaseTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private AccountService accountService;

    @Mock
    private LedgerService ledgerService;
    @Mock
    private ApplicationEventPublisher eventPublisher; // Added

    @InjectMocks
    private ApplyPenaltyUseCase applyPenaltyUseCase;

    private Invoice overdueInvoice;
    private Invoice pendingInvoicePastDue;
    private Invoice pendingInvoiceNotDue;
    private Invoice paidInvoice;
    private Responsible responsible;
    private Account arAccount;
    private Account penaltyRevenueAccount;

    private final String invoiceId = "inv123";
    private final String responsibleId = "resp123";
    private static final BigDecimal PENALTY_AMOUNT = new BigDecimal("10.00");


    @BeforeEach
    void setUp() {
        User responsibleUser = User.builder().id("userRespTest1").username("respUserTest").build();
        responsible = Responsible.builder().id(responsibleId).name("Test Resp").user(responsibleUser).build();
        arAccount = Account.builder().id("arAcc").type(AccountType.ASSET).responsible(responsible).name("A/R - Test Resp").build();
        penaltyRevenueAccount = Account.builder().id("penaltyAcc").type(AccountType.REVENUE).name("Penalty Revenue").build();

        overdueInvoice = Invoice.builder()
                .id(invoiceId)
                .responsible(responsible)
                .status(InvoiceStatus.OVERDUE)
                .dueDate(LocalDate.now(ZoneId.of("America/Sao_Paulo")).minusDays(1))
                .originalAmount(new BigDecimal("100"))
                .build();

        pendingInvoicePastDue = Invoice.builder()
                .id("invPendingPastDue")
                .responsible(responsible)
                .status(InvoiceStatus.PENDING)
                .dueDate(LocalDate.now(ZoneId.of("America/Sao_Paulo")).minusDays(1))
                .originalAmount(new BigDecimal("200"))
                .build();
        
        pendingInvoiceNotDue = Invoice.builder()
                .id("invPendingNotDue")
                .responsible(responsible)
                .status(InvoiceStatus.PENDING)
                .dueDate(LocalDate.now(ZoneId.of("America/Sao_Paulo")).plusDays(5))
                .originalAmount(new BigDecimal("300"))
                .build();

        paidInvoice = Invoice.builder()
                .id("invPaid")
                .responsible(responsible)
                .status(InvoiceStatus.PAID)
                .dueDate(LocalDate.now(ZoneId.of("America/Sao_Paulo")).minusDays(10))
                .originalAmount(new BigDecimal("100"))
                .build();
    }

    private void setupMocksForSuccessfulPenalty() {
        when(accountService.findOrCreateResponsibleARAccount(responsibleId)).thenReturn(arAccount);
        when(accountService.findOrCreateAccount(eq("Penalty Revenue"), eq(AccountType.REVENUE), eq(null)))
                .thenReturn(penaltyRevenueAccount);
        doNothing().when(ledgerService).postTransaction(
                any(Invoice.class), eq(null), eq(arAccount), eq(penaltyRevenueAccount), eq(PENALTY_AMOUNT),
                any(LocalDateTime.class), anyString(), eq(LedgerEntryType.PENALTY_ASSESSED)
        );
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(eventPublisher).publishEvent(any(PenaltyAssessedEvent.class)); // Added
    }


    @Test
    void execute_ShouldApplyPenalty_WhenInvoiceIsOverdue() {
        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(overdueInvoice));
        setupMocksForSuccessfulPenalty();

        assertDoesNotThrow(() -> applyPenaltyUseCase.execute(invoiceId));

        verify(ledgerService).postTransaction(
                eq(overdueInvoice), eq(null), eq(arAccount), eq(penaltyRevenueAccount), eq(PENALTY_AMOUNT),
                any(LocalDateTime.class), eq("Penalty assessed for overdue Invoice #" + invoiceId), eq(LedgerEntryType.PENALTY_ASSESSED)
        );
        verify(invoiceRepository).save(overdueInvoice); 
        verify(eventPublisher).publishEvent(any(PenaltyAssessedEvent.class));
    }

    @Test
    void execute_ShouldApplyPenaltyAndUpdateStatus_WhenInvoiceIsPendingAndPastDue() {
        when(invoiceRepository.findById(pendingInvoicePastDue.getId())).thenReturn(Optional.of(pendingInvoicePastDue));
        setupMocksForSuccessfulPenalty();
        
        assertDoesNotThrow(() -> applyPenaltyUseCase.execute(pendingInvoicePastDue.getId()));

        assertEquals(InvoiceStatus.OVERDUE, pendingInvoicePastDue.getStatus());
        verify(ledgerService).postTransaction(
                eq(pendingInvoicePastDue), eq(null), eq(arAccount), eq(penaltyRevenueAccount), eq(PENALTY_AMOUNT),
                any(LocalDateTime.class), anyString(), eq(LedgerEntryType.PENALTY_ASSESSED)
        );
        verify(invoiceRepository).save(pendingInvoicePastDue); 
        verify(eventPublisher).publishEvent(any(PenaltyAssessedEvent.class));
    }

    @Test
    void execute_ShouldThrowBusinessException_WhenInvoiceIsPendingAndNotDue() {
        when(invoiceRepository.findById(pendingInvoiceNotDue.getId())).thenReturn(Optional.of(pendingInvoiceNotDue));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> applyPenaltyUseCase.execute(pendingInvoiceNotDue.getId()));
        assertTrue(exception.getMessage().contains("is not eligible for penalty application"));
        verifyNoInteractions(ledgerService);
        verifyNoInteractions(eventPublisher);
    }
    
    @Test
    void execute_ShouldThrowBusinessException_WhenInvoiceIsPaid() {
        when(invoiceRepository.findById(paidInvoice.getId())).thenReturn(Optional.of(paidInvoice));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> applyPenaltyUseCase.execute(paidInvoice.getId()));
        assertTrue(exception.getMessage().contains("is not eligible for penalty application"));
        verifyNoInteractions(ledgerService);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void execute_ShouldThrowResourceNotFoundException_WhenInvoiceNotFound() {
        String nonExistentId = "nonExistentId";
        when(invoiceRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> applyPenaltyUseCase.execute(nonExistentId));
        verifyNoInteractions(accountService);
        verifyNoInteractions(ledgerService);
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
        overdueInvoice.setResponsible(Responsible.builder().id(responsibleId).user(null).build());
        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(overdueInvoice));
        BusinessException ex2 = assertThrows(BusinessException.class, () -> applyPenaltyUseCase.execute(invoiceId));
        assertEquals("Invoice responsible or user details not found. Cannot apply penalty.", ex2.getMessage());
        
        verifyNoInteractions(ledgerService);
        verifyNoInteractions(eventPublisher);
    }
}
