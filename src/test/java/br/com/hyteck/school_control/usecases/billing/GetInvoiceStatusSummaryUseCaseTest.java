package br.com.hyteck.school_control.usecases.billing;

import br.com.hyteck.school_control.models.finance.Account;
import br.com.hyteck.school_control.models.finance.AccountType;
import br.com.hyteck.school_control.models.payments.*;
import br.com.hyteck.school_control.repositories.InvoiceRepository;
import br.com.hyteck.school_control.repositories.LedgerEntryRepository;
import br.com.hyteck.school_control.repositories.PaymentRepository;
import br.com.hyteck.school_control.services.AccountService;
import br.com.hyteck.school_control.web.dtos.billing.InvoiceBasicInfo;
import br.com.hyteck.school_control.web.dtos.billing.InvoiceStatusSummaryDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetInvoiceStatusSummaryUseCaseTest {

    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private LedgerEntryRepository ledgerEntryRepository;
    @Mock
    private AccountService accountService;
    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private GetInvoiceStatusSummaryUseCase getInvoiceStatusSummaryUseCase;

    private YearMonth targetPeriod;
    private Responsible responsible1;
    private Account arAccountResp1;

    @BeforeEach
    void setUp() {
        targetPeriod = YearMonth.of(2023, 10); // October 2023
        responsible1 = Responsible.builder().id("resp1").name("Responsible One").build();
        arAccountResp1 = Account.builder().id("ar1").type(AccountType.ASSET).responsible(responsible1).name("A/R - Responsible One").build();
    }

    private Invoice createInvoice(String id, Responsible responsible, InvoiceStatus status, BigDecimal originalAmount, LocalDate dueDate) {
        return Invoice.builder()
                .id(id)
                .responsible(responsible)
                .referenceMonth(targetPeriod) // Can be different if needed for specific test cases
                .status(status)
                .originalAmount(originalAmount)
                .dueDate(dueDate)
                .issueDate(dueDate.minusDays(30))
                .items(new ArrayList<>()) // Assuming items are not directly used in summary logic beyond original amount
                .build();
    }

    // Tests for executeOverallSummary()
    @Test
    void executeOverallSummary_noPendingOrOverdueInvoices_shouldReturnEmptySummary() {
        when(invoiceRepository.findByStatus(InvoiceStatus.PENDING)).thenReturn(Collections.emptyList());
        when(invoiceRepository.findByStatus(InvoiceStatus.OVERDUE)).thenReturn(Collections.emptyList());

        InvoiceStatusSummaryDto result = getInvoiceStatusSummaryUseCase.executeOverallSummary();

        assertEquals(0, result.getTotalPendingInvoices());
        assertEquals(BigDecimal.ZERO, result.getTotalPendingBalance());
        assertTrue(result.getPendingInvoicesList().isEmpty());
        assertEquals(0, result.getTotalOverdueInvoices());
        assertEquals(BigDecimal.ZERO, result.getTotalOverdueBalance());
        assertTrue(result.getOverdueInvoicesList().isEmpty());
        assertEquals(0, result.getTotalPaidOnTimeInPeriod());
        assertEquals(0, result.getTotalPaidLateInPeriod());
    }

    @Test
    void executeOverallSummary_pendingAndOverdueInvoicesExist_shouldReturnCorrectSummary() {
        Invoice pendingInvoice = createInvoice("invPend1", responsible1, InvoiceStatus.PENDING, new BigDecimal("200"), targetPeriod.atDay(15));
        Invoice overdueInvoice = createInvoice("invOver1", responsible1, InvoiceStatus.OVERDUE, new BigDecimal("300"), targetPeriod.atDay(1).minusDays(5));

        when(invoiceRepository.findByStatus(InvoiceStatus.PENDING)).thenReturn(List.of(pendingInvoice));
        when(invoiceRepository.findByStatus(InvoiceStatus.OVERDUE)).thenReturn(List.of(overdueInvoice));

        when(accountService.findOrCreateResponsibleARAccount(responsible1.getId())).thenReturn(arAccountResp1);
        when(ledgerEntryRepository.getBalanceForInvoiceOnAccount(arAccountResp1.getId(), pendingInvoice.getId()))
                .thenReturn(new BigDecimal("180.00")); // Ledger balance
        when(ledgerEntryRepository.getBalanceForInvoiceOnAccount(arAccountResp1.getId(), overdueInvoice.getId()))
                .thenReturn(new BigDecimal("250.00")); // Ledger balance

        InvoiceStatusSummaryDto result = getInvoiceStatusSummaryUseCase.executeOverallSummary();

        assertEquals(1, result.getTotalPendingInvoices());
        assertEquals(new BigDecimal("180.00"), result.getTotalPendingBalance());
        assertEquals(1, result.getPendingInvoicesList().size());
        assertEquals("invPend1", result.getPendingInvoicesList().get(0).id());
        assertEquals(new BigDecimal("180.00"), result.getPendingInvoicesList().get(0).currentBalanceDue());

        assertEquals(1, result.getTotalOverdueInvoices());
        assertEquals(new BigDecimal("250.00"), result.getTotalOverdueBalance());
        assertEquals(1, result.getOverdueInvoicesList().size());
        assertEquals("invOver1", result.getOverdueInvoicesList().get(0).id());
        assertEquals(new BigDecimal("250.00"), result.getOverdueInvoicesList().get(0).currentBalanceDue());
    }

    // Tests for executePaidInvoiceSummaryForPeriod()
    @Test
    void executePaidInvoiceSummaryForPeriod_noPaymentsInPeriod_shouldReturnZeroCounts() {
        LocalDate startDate = targetPeriod.atDay(1);
        LocalDate endDate = targetPeriod.atEndOfMonth();
        when(paymentRepository.findByPaymentDateBetween(startDate, endDate)).thenReturn(Collections.emptyList());

        InvoiceStatusSummaryDto result = getInvoiceStatusSummaryUseCase.executePaidInvoiceSummaryForPeriod(targetPeriod);

        assertEquals(0, result.getTotalPaidOnTimeInPeriod());
        assertEquals(0, result.getTotalPaidLateInPeriod());
        // Overall fields should also be zero/empty
        assertEquals(0, result.getTotalPendingInvoices());
        assertTrue(result.getOverdueInvoicesList().isEmpty());
    }

    @Test
    void executePaidInvoiceSummaryForPeriod_paymentsExist_shouldClassifyCorrectly() {
        LocalDate startDate = targetPeriod.atDay(1);
        LocalDate endDate = targetPeriod.atEndOfMonth();

        Invoice invoicePaidOnTime = createInvoice("invPaid1", responsible1, InvoiceStatus.PAID, new BigDecimal("100"), targetPeriod.atDay(10));
        Payment paymentOnTime = Payment.builder().id("pay1").invoice(invoicePaidOnTime).paymentDate(targetPeriod.atDay(5).atStartOfDay()).amountPaid(new BigDecimal("100")).build();

        Invoice invoicePaidLate = createInvoice("invPaid2", responsible1, InvoiceStatus.PAID, new BigDecimal("150"), targetPeriod.atDay(2));
        Payment paymentLate = Payment.builder().id("pay2").invoice(invoicePaidLate).paymentDate(targetPeriod.atDay(3).atStartOfDay()).amountPaid(new BigDecimal("150")).build();
        
        Invoice invoicePaidExactlyOnDueDate = createInvoice("invPaid3", responsible1, InvoiceStatus.PAID, new BigDecimal("200"), targetPeriod.atDay(7));
        Payment paymentExactlyOnTime = Payment.builder().id("pay3").invoice(invoicePaidExactlyOnDueDate).paymentDate(targetPeriod.atDay(7).atTime(10,0)).amountPaid(new BigDecimal("200")).build();


        when(paymentRepository.findByPaymentDateBetween(startDate, endDate)).thenReturn(List.of(paymentOnTime, paymentLate, paymentExactlyOnTime));

        InvoiceStatusSummaryDto result = getInvoiceStatusSummaryUseCase.executePaidInvoiceSummaryForPeriod(targetPeriod);

        assertEquals(2, result.getTotalPaidOnTimeInPeriod()); // paymentOnTime, paymentExactlyOnTime
        assertEquals(1, result.getTotalPaidLateInPeriod());   // paymentLate
    }
    
    @Test
    void executePaidInvoiceSummaryForPeriod_paymentWithNoInvoice_shouldSkipAndLog() {
        LocalDate startDate = targetPeriod.atDay(1);
        LocalDate endDate = targetPeriod.atEndOfMonth();
        
        Payment paymentNoInvoice = Payment.builder().id("payNoInv").invoice(null).paymentDate(targetPeriod.atDay(5).atStartOfDay()).amountPaid(new BigDecimal("50")).build();
        
        when(paymentRepository.findByPaymentDateBetween(startDate, endDate)).thenReturn(List.of(paymentNoInvoice));
        
        InvoiceStatusSummaryDto result = getInvoiceStatusSummaryUseCase.executePaidInvoiceSummaryForPeriod(targetPeriod);
        
        assertEquals(0, result.getTotalPaidOnTimeInPeriod());
        assertEquals(0, result.getTotalPaidLateInPeriod());
    }
    
    @Test
    void executePaidInvoiceSummaryForPeriod_invoiceNotActuallyPAID_shouldStillConsiderPaymentAndLogWarning() {
        // This test assumes that if a Payment record exists for an invoice within the period,
        // it's counted, but a warning might be logged if the invoice status isn't PAID.
        // The current implementation of GetInvoiceStatusSummaryUseCase logs this.
        LocalDate startDate = targetPeriod.atDay(1);
        LocalDate endDate = targetPeriod.atEndOfMonth();

        Invoice invoiceStillPending = createInvoice("invStillPend", responsible1, InvoiceStatus.PENDING, // Status is not PAID
                                                new BigDecimal("100"), targetPeriod.atDay(10));
        Payment paymentForPending = Payment.builder().id("payPend").invoice(invoiceStillPending)
                                        .paymentDate(targetPeriod.atDay(5).atStartOfDay()) // Paid on time
                                        .amountPaid(new BigDecimal("100")).build();
        
        when(paymentRepository.findByPaymentDateBetween(startDate, endDate)).thenReturn(List.of(paymentForPending));
        
        InvoiceStatusSummaryDto result = getInvoiceStatusSummaryUseCase.executePaidInvoiceSummaryForPeriod(targetPeriod);
        
        assertEquals(1, result.getTotalPaidOnTimeInPeriod());
        assertEquals(0, result.getTotalPaidLateInPeriod());
        // Logger verification would be needed for the warning, which is complex in unit tests without specific setup.
        // For now, focusing on the counting logic.
    }
}
