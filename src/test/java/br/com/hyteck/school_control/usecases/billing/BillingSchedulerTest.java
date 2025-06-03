package br.com.hyteck.school_control.usecases.billing;

import br.com.hyteck.school_control.models.payments.Invoice;
import br.com.hyteck.school_control.models.payments.InvoiceStatus;
import br.com.hyteck.school_control.repositories.InvoiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BillingSchedulerTest {

    @Mock
    private GenerateInvoicesForParents generateInvoicesForParents;
    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private ApplyPenaltyUseCase applyPenaltyUseCase;
    @Mock
    private UpdateInvoiceStatusUseCase updateInvoiceStatusUseCase;
    @Mock
    private PlatformTransactionManager transactionManager; // Mock TransactionManager

    @InjectMocks
    private BillingScheduler billingScheduler;

    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void setUp() {
        // Setup TransactionTemplate to work with the mocked PlatformTransactionManager
        // This allows us to control the execution of the transaction callback.
        transactionTemplate = new TransactionTemplate(transactionManager);
        // For tests where the transaction needs to execute the callback:
//        doAnswer(invocation -> {
//            TransactionCallbackWithoutResult callback = invocation.getArgument(0);
//            // Simulate transaction execution by directly invoking the callback's doInTransactionWithoutResult method
//            callback.doInTransaction(mock(TransactionStatus.class));
//            return null; // For TransactionCallbackWithoutResult, return null
//        }).when(transactionManager).execute(any(TransactionCallbackWithoutResult.class));
        
        // Re-inject mocks if BillingScheduler uses constructor injection for TransactionTemplate,
        // or set it manually if it has a setter (not the case here).
        // Since BillingScheduler creates its own TransactionTemplate, we can't directly inject a mock one.
        // So, the above mocking of PlatformTransactionManager is key.
        // The BillingScheduler will internally create a new TransactionTemplate(transactionManager)
        // and our mocked transactionManager will be used by it.
    }

    @Test
    void generateMonthlyInvoicesScheduled_ShouldCallGenerateInvoicesForParents() {
        // Arrange
        YearMonth currentMonth = YearMonth.now(ZoneId.of("America/Sao_Paulo"));
        doNothing().when(generateInvoicesForParents).execute(currentMonth);

        // Act
        billingScheduler.generateMonthlyInvoicesScheduled();

        // Assert
        verify(generateInvoicesForParents).execute(currentMonth);
    }

    @Test
    void generateMonthlyInvoicesScheduled_ShouldHandleExceptionFromUseCase() {
        // Arrange
        YearMonth currentMonth = YearMonth.now(ZoneId.of("America/Sao_Paulo"));
        doThrow(new RuntimeException("Test Error")).when(generateInvoicesForParents).execute(currentMonth);

        // Act
        // We expect the scheduler to catch and log the exception, not rethrow it.
        assertDoesNotThrow(() -> billingScheduler.generateMonthlyInvoicesScheduled());

        // Assert
        verify(generateInvoicesForParents).execute(currentMonth);
        // Log verification could be added if a testable logger was injected.
    }

    @Test
    void processOverdueInvoicesAndStatusUpdates_ShouldDoNothing_WhenNoInvoicesToProcess() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 100);
        Page<Invoice> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
        when(invoiceRepository.findByStatusInAndDueDateBefore(
                anyList(), any(LocalDate.class), any(Pageable.class)))
                .thenReturn(emptyPage);

        // Act
        billingScheduler.processOverdueInvoicesAndStatusUpdates();

        // Assert
        verify(invoiceRepository).findByStatusInAndDueDateBefore(anyList(), any(LocalDate.class), any(Pageable.class));
        verifyNoInteractions(applyPenaltyUseCase);
        verifyNoInteractions(updateInvoiceStatusUseCase);
    }

    @Test
    void processOverdueInvoicesAndStatusUpdates_ShouldProcessPendingPastDueInvoice() {
        // Arrange
        LocalDate today = LocalDate.now(ZoneId.of("America/Sao_Paulo"));
        Invoice pendingPastDueInvoice = Invoice.builder()
                .id("inv1")
                .status(InvoiceStatus.PENDING)
                .dueDate(today.minusDays(1))
                .build();
        Pageable pageable = PageRequest.of(0, 100);
        Page<Invoice> pageWithOneInvoice = new PageImpl<>(List.of(pendingPastDueInvoice), pageable, 1);

        when(invoiceRepository.findByStatusInAndDueDateBefore(
                eq(List.of(InvoiceStatus.PENDING, InvoiceStatus.OVERDUE)), eq(today), eq(pageable)))
                .thenReturn(pageWithOneInvoice);
        // Simulate no more pages after this one for simplicity
        when(invoiceRepository.findByStatusInAndDueDateBefore(
                anyList(), any(LocalDate.class), eq(pageWithOneInvoice.nextPageable())))
                .thenReturn(Page.empty(pageWithOneInvoice.nextPageable()));

        doNothing().when(applyPenaltyUseCase).execute("inv1");
        when(updateInvoiceStatusUseCase.execute("inv1")).thenReturn(pendingPastDueInvoice); // or mock updated invoice

        // Act
        billingScheduler.processOverdueInvoicesAndStatusUpdates();

        // Assert
        // Verify that execute is called within the TransactionTemplate
        // This relies on the transactionManager mock correctly invoking the callback.
        verify(applyPenaltyUseCase).execute("inv1");
        verify(updateInvoiceStatusUseCase).execute("inv1");
    }
    
    @Test
    void processOverdueInvoicesAndStatusUpdates_ShouldProcessOverdueInvoice_NoPenaltyCall() {
        LocalDate today = LocalDate.now(ZoneId.of("America/Sao_Paulo"));
        Invoice overdueInvoice = Invoice.builder()
                .id("inv2")
                .status(InvoiceStatus.OVERDUE) // Already overdue
                .dueDate(today.minusDays(5))
                .build();
        Pageable pageable = PageRequest.of(0, 100);
        Page<Invoice> page = new PageImpl<>(List.of(overdueInvoice), pageable, 1);

        when(invoiceRepository.findByStatusInAndDueDateBefore(anyList(), any(LocalDate.class), eq(pageable))).thenReturn(page);
        when(invoiceRepository.findByStatusInAndDueDateBefore(anyList(), any(LocalDate.class), eq(page.nextPageable()))).thenReturn(Page.empty(page.nextPageable()));
        when(updateInvoiceStatusUseCase.execute("inv2")).thenReturn(overdueInvoice);

        billingScheduler.processOverdueInvoicesAndStatusUpdates();

        verify(applyPenaltyUseCase, never()).execute("inv2"); // Should not be called if already OVERDUE
        verify(updateInvoiceStatusUseCase).execute("inv2");
    }


    @Test
    void processOverdueInvoicesAndStatusUpdates_ShouldHandleExceptionDuringPenaltyApplication_AndContinue() {
        LocalDate today = LocalDate.now(ZoneId.of("America/Sao_Paulo"));
        Invoice invoice1 = Invoice.builder().id("inv1").status(InvoiceStatus.PENDING).dueDate(today.minusDays(1)).build();
        Invoice invoice2 = Invoice.builder().id("inv2").status(InvoiceStatus.PENDING).dueDate(today.minusDays(1)).build();
        Pageable pageable = PageRequest.of(0, 100);
        Page<Invoice> page = new PageImpl<>(List.of(invoice1, invoice2), pageable, 2);

        when(invoiceRepository.findByStatusInAndDueDateBefore(anyList(), any(LocalDate.class), eq(pageable))).thenReturn(page);
        when(invoiceRepository.findByStatusInAndDueDateBefore(anyList(), any(LocalDate.class), eq(page.nextPageable()))).thenReturn(Page.empty(page.nextPageable()));

        // Simulate error for invoice1's penalty
        doThrow(new RuntimeException("Penalty Error for inv1")).when(applyPenaltyUseCase).execute("inv1");
        // Successful penalty and status update for invoice2
        doNothing().when(applyPenaltyUseCase).execute("inv2");
        when(updateInvoiceStatusUseCase.execute(anyString())).thenAnswer(inv -> inv.getArgument(0).equals("inv1") ? invoice1 : invoice2);


        billingScheduler.processOverdueInvoicesAndStatusUpdates();

        // Verify for invoice1 (penalty failed, status update still called)
        verify(applyPenaltyUseCase).execute("inv1");
        verify(updateInvoiceStatusUseCase).execute("inv1");

        // Verify for invoice2 (penalty succeeded, status update called)
        verify(applyPenaltyUseCase).execute("inv2");
        verify(updateInvoiceStatusUseCase).execute("inv2");
    }
    
    @Test
    void processOverdueInvoicesAndStatusUpdates_ShouldHandlePagination() {
        LocalDate today = LocalDate.now(ZoneId.of("America/Sao_Paulo"));
        Invoice invoice1 = Invoice.builder().id("inv1").status(InvoiceStatus.PENDING).dueDate(today.minusDays(1)).build();
        Invoice invoice2 = Invoice.builder().id("inv2").status(InvoiceStatus.OVERDUE).dueDate(today.minusDays(2)).build();

        Pageable firstPageable = PageRequest.of(0, 1); // Page size 1
        Pageable secondPageable = PageRequest.of(1, 1);

        Page<Invoice> firstPageResult = new PageImpl<>(List.of(invoice1), firstPageable, 2); // Total 2 elements
        Page<Invoice> secondPageResult = new PageImpl<>(List.of(invoice2), secondPageable, 2);
        Page<Invoice> emptyPage = Page.empty(PageRequest.of(2,1));


        when(invoiceRepository.findByStatusInAndDueDateBefore(anyList(), any(LocalDate.class), eq(firstPageable)))
            .thenReturn(firstPageResult);
        when(invoiceRepository.findByStatusInAndDueDateBefore(anyList(), any(LocalDate.class), eq(secondPageable)))
            .thenReturn(secondPageResult);
         when(invoiceRepository.findByStatusInAndDueDateBefore(anyList(), any(LocalDate.class), eq(emptyPage.getPageable())))
            .thenReturn(emptyPage);


        doNothing().when(applyPenaltyUseCase).execute(anyString());
        when(updateInvoiceStatusUseCase.execute(anyString())).thenAnswer(inv -> {
            String id = inv.getArgument(0);
            if(id.equals("inv1")) return invoice1;
            if(id.equals("inv2")) return invoice2;
            return null;
        });

        billingScheduler.processOverdueInvoicesAndStatusUpdates();

        verify(applyPenaltyUseCase).execute("inv1");
        verify(updateInvoiceStatusUseCase).execute("inv1");
        // For invoice2, applyPenaltyUseCase is not called as it's already OVERDUE
        verify(applyPenaltyUseCase, never()).execute("inv2"); 
        verify(updateInvoiceStatusUseCase).execute("inv2");
        
        verify(invoiceRepository, times(3)).findByStatusInAndDueDateBefore(anyList(), any(LocalDate.class), any(Pageable.class));
    }

}
