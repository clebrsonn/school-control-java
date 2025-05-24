package br.com.hyteck.school_control.services;

import br.com.hyteck.school_control.models.finance.Account;
import br.com.hyteck.school_control.models.finance.AccountType;
import br.com.hyteck.school_control.models.payments.Invoice;
import br.com.hyteck.school_control.models.payments.InvoiceStatus;
import br.com.hyteck.school_control.models.payments.Responsible;
import br.com.hyteck.school_control.repositories.InvoiceRepository;
import br.com.hyteck.school_control.repositories.LedgerEntryRepository; // Corrected path if needed
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;


@ExtendWith(MockitoExtension.class)
class InvoiceCalculationServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private AccountService accountService; // Added mock
    @Mock
    private LedgerEntryRepository ledgerEntryRepository; // Added mock

    @InjectMocks
    private InvoiceCalculationService invoiceCalculationService;

    private YearMonth targetMonth;
    private Responsible responsible1;
    private Account arAccountResp1;

    @BeforeEach
    void setUp() {
        targetMonth = YearMonth.of(2023, 8); // August 2023
        responsible1 = Responsible.builder().id("resp1").name("Responsible One").build();
        arAccountResp1 = Account.builder().id("ar1").type(AccountType.ASSET).responsible(responsible1).name("A/R - Responsible One").build();
    }

    private Invoice createInvoice(String id, Responsible responsible, InvoiceStatus status, BigDecimal originalAmount) {
        return Invoice.builder()
                .id(id)
                .responsible(responsible)
                .referenceMonth(targetMonth)
                .status(status)
                .originalAmount(originalAmount) 
                .dueDate(targetMonth.atDay(10))
                .issueDate(targetMonth.atDay(1).minusMonths(1)) 
                .build();
    }

    @Test
    void calcularTotalAReceberNoMes_noOpenOrOverdueInvoices_shouldReturnZero() {
        // Arrange
        List<InvoiceStatus> expectedStatuses = Arrays.asList(InvoiceStatus.PENDING, InvoiceStatus.OVERDUE);
        when(invoiceRepository.findPendingInvoicesByMonth(eq(targetMonth), eq(expectedStatuses))).thenReturn(Collections.emptyList());

        // Act
        BigDecimal total = invoiceCalculationService.calcularTotalAReceberNoMes(targetMonth);

        // Assert
        assertEquals(BigDecimal.ZERO, total);
        verify(ledgerEntryRepository, never()).getBalanceForInvoiceOnAccount(any(), any());
        verify(accountService, never()).findOrCreateResponsibleARAccount(any());
    }

    @Test
    void calcularTotalAReceberNoMes_oneOpenInvoice_shouldReturnItsLedgerBalance() {
        // Arrange
        Invoice invoice1 = createInvoice("inv1", responsible1, InvoiceStatus.PENDING, new BigDecimal("500"));
        List<Invoice> invoices = List.of(invoice1);
        List<InvoiceStatus> expectedStatuses = Arrays.asList(InvoiceStatus.PENDING, InvoiceStatus.OVERDUE);


        when(invoiceRepository.findPendingInvoicesByMonth(targetMonth, expectedStatuses)).thenReturn(invoices);
        when(accountService.findOrCreateResponsibleARAccount(responsible1.getId())).thenReturn(arAccountResp1);
        when(ledgerEntryRepository.getBalanceForInvoiceOnAccount(arAccountResp1.getId(), invoice1.getId()))
                .thenReturn(new BigDecimal("450.00")); // Ledger balance might differ from original

        // Act
        BigDecimal total = invoiceCalculationService.calcularTotalAReceberNoMes(targetMonth);

        // Assert
        assertEquals(new BigDecimal("450.00"), total);
        verify(invoiceRepository).findPendingInvoicesByMonth(targetMonth, expectedStatuses);
        verify(accountService).findOrCreateResponsibleARAccount(responsible1.getId());
        verify(ledgerEntryRepository).getBalanceForInvoiceOnAccount(arAccountResp1.getId(), invoice1.getId());
    }

    @Test
    void calcularTotalAReceberNoMes_multipleOpenAndOverdueInvoices_shouldReturnSumOfLedgerBalances() {
        // Arrange
        Responsible responsible2 = Responsible.builder().id("resp2").name("Responsible Two").build();
        Account arAccountResp2 = Account.builder().id("ar2").type(AccountType.ASSET).responsible(responsible2).name("A/R - Responsible Two").build();

        Invoice invoice1 = createInvoice("inv1", responsible1, InvoiceStatus.PENDING, new BigDecimal("300"));
        Invoice invoice2 = createInvoice("inv2", responsible2, InvoiceStatus.OVERDUE, new BigDecimal("200"));
        Invoice invoice3 = createInvoice("inv3", responsible1, InvoiceStatus.PENDING, new BigDecimal("100")); // Another for resp1
        List<Invoice> invoices = List.of(invoice1, invoice2, invoice3);
        List<InvoiceStatus> expectedStatuses = Arrays.asList(InvoiceStatus.PENDING, InvoiceStatus.OVERDUE);

        when(invoiceRepository.findPendingInvoicesByMonth(targetMonth, expectedStatuses)).thenReturn(invoices);

        when(accountService.findOrCreateResponsibleARAccount(responsible1.getId())).thenReturn(arAccountResp1);
        when(accountService.findOrCreateResponsibleARAccount(responsible2.getId())).thenReturn(arAccountResp2);

        when(ledgerEntryRepository.getBalanceForInvoiceOnAccount(arAccountResp1.getId(), invoice1.getId()))
                .thenReturn(new BigDecimal("250.00")); 
        when(ledgerEntryRepository.getBalanceForInvoiceOnAccount(arAccountResp2.getId(), invoice2.getId()))
                .thenReturn(new BigDecimal("180.00")); 
        when(ledgerEntryRepository.getBalanceForInvoiceOnAccount(arAccountResp1.getId(), invoice3.getId()))
                .thenReturn(new BigDecimal("50.00"));  

        // Act
        BigDecimal total = invoiceCalculationService.calcularTotalAReceberNoMes(targetMonth);

        // Assert
        assertEquals(new BigDecimal("480.00"), total, "Total should be sum of ledger balances (250+180+50)");
        verify(invoiceRepository).findPendingInvoicesByMonth(targetMonth, expectedStatuses);
        verify(accountService, times(2)).findOrCreateResponsibleARAccount(responsible1.getId()); // Called for inv1 and inv3
        verify(accountService).findOrCreateResponsibleARAccount(responsible2.getId()); // Called for inv2
        verify(ledgerEntryRepository).getBalanceForInvoiceOnAccount(arAccountResp1.getId(), invoice1.getId());
        verify(ledgerEntryRepository).getBalanceForInvoiceOnAccount(arAccountResp2.getId(), invoice2.getId());
        verify(ledgerEntryRepository).getBalanceForInvoiceOnAccount(arAccountResp1.getId(), invoice3.getId());
    }

    @Test
    void calcularTotalAReceberNoMes_invoiceWithZeroBalanceOnLedger_shouldIncludeItInSumCorrectly() {
        // Arrange
        Invoice invoice1 = createInvoice("inv1", responsible1, InvoiceStatus.PENDING, new BigDecimal("100"));
        Invoice invoice2 = createInvoice("inv2", responsible1, InvoiceStatus.OVERDUE, new BigDecimal("200"));
        List<Invoice> invoices = List.of(invoice1, invoice2);
        List<InvoiceStatus> expectedStatuses = Arrays.asList(InvoiceStatus.PENDING, InvoiceStatus.OVERDUE);

        when(invoiceRepository.findPendingInvoicesByMonth(targetMonth, expectedStatuses)).thenReturn(invoices);
        when(accountService.findOrCreateResponsibleARAccount(responsible1.getId())).thenReturn(arAccountResp1);
        
        when(ledgerEntryRepository.getBalanceForInvoiceOnAccount(arAccountResp1.getId(), invoice1.getId()))
                .thenReturn(BigDecimal.ZERO); // Invoice is pending but fully paid off or credited on ledger
        when(ledgerEntryRepository.getBalanceForInvoiceOnAccount(arAccountResp1.getId(), invoice2.getId()))
                .thenReturn(new BigDecimal("150.00"));


        // Act
        BigDecimal total = invoiceCalculationService.calcularTotalAReceberNoMes(targetMonth);

        // Assert
        assertEquals(new BigDecimal("150.00"), total, "Total should be sum of (0 + 150)");
        verify(ledgerEntryRepository).getBalanceForInvoiceOnAccount(arAccountResp1.getId(), invoice1.getId());
        verify(ledgerEntryRepository).getBalanceForInvoiceOnAccount(arAccountResp1.getId(), invoice2.getId());
    }

    @Test
    void calcularTotalAReceberNoMes_invoiceWithNoResponsible_shouldSkipAndLogWarning() {
        // Arrange
        Invoice invoiceNoResp = createInvoice("invNoResp", null, InvoiceStatus.PENDING, new BigDecimal("100"));
        invoiceNoResp.setResponsible(null); // Ensure responsible is null

        Invoice invoiceValid = createInvoice("invValid", responsible1, InvoiceStatus.PENDING, new BigDecimal("200"));
        List<Invoice> invoices = List.of(invoiceNoResp, invoiceValid);
        List<InvoiceStatus> expectedStatuses = Arrays.asList(InvoiceStatus.PENDING, InvoiceStatus.OVERDUE);

        when(invoiceRepository.findPendingInvoicesByMonth(targetMonth, expectedStatuses)).thenReturn(invoices);
        // For invoiceValid
        when(accountService.findOrCreateResponsibleARAccount(responsible1.getId())).thenReturn(arAccountResp1);
        when(ledgerEntryRepository.getBalanceForInvoiceOnAccount(arAccountResp1.getId(), invoiceValid.getId()))
                .thenReturn(new BigDecimal("150.00"));

        // Act
        BigDecimal total = invoiceCalculationService.calcularTotalAReceberNoMes(targetMonth);

        // Assert
        assertEquals(new BigDecimal("150.00"), total, "Only the valid invoice's balance should be summed");
        verify(accountService).findOrCreateResponsibleARAccount(responsible1.getId()); // Called only for valid invoice
        verify(accountService, never()).findOrCreateResponsibleARAccount(null);
        verify(ledgerEntryRepository).getBalanceForInvoiceOnAccount(arAccountResp1.getId(), invoiceValid.getId());
        verify(ledgerEntryRepository, never()).getBalanceForInvoiceOnAccount(any(), eq(invoiceNoResp.getId()));
    }
    
    @Test
    void calcularTotalAReceberNoMes_errorFetchingARAccount_shouldLogErrorAndSkipInvoice() {
        // Arrange
        Invoice invoice1 = createInvoice("inv1", responsible1, InvoiceStatus.PENDING, new BigDecimal("300"));
        Responsible responsible2 = Responsible.builder().id("resp2").name("Responsible Two").build();
        Invoice invoice2 = createInvoice("inv2", responsible2, InvoiceStatus.OVERDUE, new BigDecimal("200")); // This one will cause error
        Invoice invoice3 = createInvoice("inv3", responsible1, InvoiceStatus.PENDING, new BigDecimal("100"));

        List<Invoice> invoices = List.of(invoice1, invoice2, invoice3);
        List<InvoiceStatus> expectedStatuses = Arrays.asList(InvoiceStatus.PENDING, InvoiceStatus.OVERDUE);

        when(invoiceRepository.findPendingInvoicesByMonth(targetMonth, expectedStatuses)).thenReturn(invoices);

        // Normal behavior for inv1 and inv3
        when(accountService.findOrCreateResponsibleARAccount(responsible1.getId())).thenReturn(arAccountResp1);
        when(ledgerEntryRepository.getBalanceForInvoiceOnAccount(arAccountResp1.getId(), invoice1.getId()))
                .thenReturn(new BigDecimal("250.00"));
        when(ledgerEntryRepository.getBalanceForInvoiceOnAccount(arAccountResp1.getId(), invoice3.getId()))
                .thenReturn(new BigDecimal("50.00"));

        // Error for inv2
        when(accountService.findOrCreateResponsibleARAccount(responsible2.getId()))
                .thenThrow(new RuntimeException("Database connection failed for resp2 A/R account"));
        
        // Act
        BigDecimal total = invoiceCalculationService.calcularTotalAReceberNoMes(targetMonth);

        // Assert
        assertEquals(new BigDecimal("300.00"), total, "Total should be sum of balances from invoices where A/R account was fetched (250+50)");
        verify(accountService).findOrCreateResponsibleARAccount(responsible1.getId()); // Called for inv1
        verify(accountService).findOrCreateResponsibleARAccount(responsible2.getId()); // Attempted for inv2
        // verify(accountService, times(1)).findOrCreateResponsibleARAccount(responsible1.getId()); // This would be better if inv1 and inv3 are grouped
                                                                                             // But current loop structure calls it per invoice.
                                                                                             // If optimization is done later, this might change.
        verify(ledgerEntryRepository, never()).getBalanceForInvoiceOnAccount(any(), eq(invoice2.getId())); // Not called for inv2 due to prior error
    }
}
