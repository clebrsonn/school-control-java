package br.com.hyteck.school_control.usecases.billing;

import br.com.hyteck.school_control.models.classrooms.Classroom;
import br.com.hyteck.school_control.models.classrooms.Enrollment;
import br.com.hyteck.school_control.models.classrooms.Student;
import br.com.hyteck.school_control.models.finance.Account;
import br.com.hyteck.school_control.models.finance.AccountType;
import br.com.hyteck.school_control.models.payments.Invoice;
import br.com.hyteck.school_control.models.payments.InvoiceItem;
import br.com.hyteck.school_control.models.payments.InvoiceStatus;
import br.com.hyteck.school_control.models.payments.Responsible;
import br.com.hyteck.school_control.repositories.InvoiceRepository;
import br.com.hyteck.school_control.repositories.LedgerEntryRepository;
import br.com.hyteck.school_control.services.AccountService;
import br.com.hyteck.school_control.web.dtos.billing.OverduePayment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class GenerateOverduePaymentsReportUseCaseTest {

    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private AccountService accountService;
    @Mock
    private LedgerEntryRepository ledgerEntryRepository;

    @InjectMocks
    private GenerateOverduePaymentsReportUseCase generateOverduePaymentsReportUseCase;

    private Responsible responsible1;
    private Account arAccountResp1;
    private Student student1;
    private Classroom classroom1;
    private Enrollment enrollment1;


    @BeforeEach
    void setUp() {
        responsible1 = Responsible.builder().id("resp1").name("Responsible One").build();
        arAccountResp1 = Account.builder().id("ar1").type(AccountType.ASSET).responsible(responsible1).name("A/R - Responsible One").build();
        student1 = Student.builder().id("stud1").name("Student One").responsible(responsible1).build();
        classroom1 = Classroom.builder().id("classA").name("Class A").build();
        enrollment1 = Enrollment.builder()
            .id("enroll1")
            .student(student1)
            .classroom(classroom1)
            .monthlyFee(new BigDecimal("100")) 
            .status(Enrollment.Status.ACTIVE)
            .build();
    }

    private Invoice createOverdueInvoice(String id, Responsible responsible, BigDecimal originalAmount, List<InvoiceItem> items) {
        Invoice invoice = Invoice.builder()
                .id(id)
                .responsible(responsible)
                .status(InvoiceStatus.OVERDUE) // Key for this use case
                .originalAmount(originalAmount)
                .dueDate(LocalDate.now().minusDays(5)) // Ensure it's overdue
                .issueDate(LocalDate.now().minusDays(35))
                .items(new ArrayList<>())
                .description("Overdue Invoice " + id)
                .build();
        items.forEach(invoice::addItem);
        return invoice;
    }
    
    private InvoiceItem createInvoiceItem(String id, Enrollment enrollment, BigDecimal amount, String description) {
        return InvoiceItem.builder()
            .id(id)
            .enrollment(enrollment)
            .amount(amount)
            .description(description)
            .build();
    }

    @Test
    void execute_noOverdueInvoices_shouldReturnEmptyList() {
        when(invoiceRepository.findByStatusAndDueDateBefore(eq(InvoiceStatus.OVERDUE), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());

        List<OverduePayment> result = generateOverduePaymentsReportUseCase.execute();

        assertTrue(result.isEmpty());
        verify(ledgerEntryRepository, never()).getBalanceForInvoiceOnAccount(any(), any());
    }

    @Test
    void execute_overdueInvoiceExists_shouldReturnOverduePaymentWithLedgerBalance() {
        InvoiceItem item1 = createInvoiceItem("item1", enrollment1, new BigDecimal("200.00"), "Overdue Tuition");
        Invoice overdueInvoice = createOverdueInvoice("ovdInv1", responsible1, new BigDecimal("200.00"), List.of(item1));
        List<Invoice> invoices = List.of(overdueInvoice);

        when(invoiceRepository.findByStatusAndDueDateBefore(eq(InvoiceStatus.OVERDUE), any(LocalDate.class)))
                .thenReturn(invoices);
        when(accountService.findOrCreateResponsibleARAccount(responsible1.getId())).thenReturn(arAccountResp1);
        when(ledgerEntryRepository.getBalanceForInvoiceOnAccount(arAccountResp1.getId(), overdueInvoice.getId()))
                .thenReturn(new BigDecimal("150.00")); // Ledger balance is different from original

        List<OverduePayment> result = generateOverduePaymentsReportUseCase.execute();

        assertEquals(1, result.size());
        OverduePayment overdueDto = result.get(0);

        assertEquals(overdueInvoice.getId(), overdueDto.invoiceId());
        assertEquals(responsible1.getName(), overdueDto.responsibleName());
        assertEquals(student1.getName(), overdueDto.studentName()); // Assuming first item's student
        assertEquals(classroom1.getName(), overdueDto.classroomName()); // Assuming first item's classroom
        assertEquals(new BigDecimal("150.00"), overdueDto.amount()); // Crucial: check ledger balance
        assertEquals(overdueInvoice.getDueDate(), overdueDto.dueDate());

        verify(ledgerEntryRepository).getBalanceForInvoiceOnAccount(arAccountResp1.getId(), overdueInvoice.getId());
    }

    @Test
    void execute_overdueInvoiceWithNoResponsible_shouldSkipAndLogWarning() {
        InvoiceItem item1 = createInvoiceItem("item1", enrollment1, new BigDecimal("100.00"), "Fee");
        Invoice invoiceNoResp = createOverdueInvoice("ovdInvNoResp", null, new BigDecimal("100.00"), List.of(item1));
        invoiceNoResp.setResponsible(null); // Ensure responsible is null
        
        List<Invoice> invoices = List.of(invoiceNoResp);

        when(invoiceRepository.findByStatusAndDueDateBefore(eq(InvoiceStatus.OVERDUE), any(LocalDate.class)))
                .thenReturn(invoices);
        // No call to accountService or ledgerEntryRepository should happen for this invoice

        List<OverduePayment> result = generateOverduePaymentsReportUseCase.execute();

        assertTrue(result.isEmpty());
        verify(accountService, never()).findOrCreateResponsibleARAccount(any());
        verify(ledgerEntryRepository, never()).getBalanceForInvoiceOnAccount(any(), any());
    }
    
    @Test
    void execute_overdueInvoiceWithZeroLedgerBalance_shouldStillBeIncluded() {
        // Requirement: "If balance is zero or less... For now, including them."
        InvoiceItem item1 = createInvoiceItem("item1", enrollment1, new BigDecimal("100.00"), "Fee");
        Invoice overdueInvoice = createOverdueInvoice("ovdInvZeroBal", responsible1, new BigDecimal("100.00"), List.of(item1));
        List<Invoice> invoices = List.of(overdueInvoice);

        when(invoiceRepository.findByStatusAndDueDateBefore(eq(InvoiceStatus.OVERDUE), any(LocalDate.class)))
                .thenReturn(invoices);
        when(accountService.findOrCreateResponsibleARAccount(responsible1.getId())).thenReturn(arAccountResp1);
        when(ledgerEntryRepository.getBalanceForInvoiceOnAccount(arAccountResp1.getId(), overdueInvoice.getId()))
                .thenReturn(BigDecimal.ZERO); // Ledger balance is zero

        List<OverduePayment> result = generateOverduePaymentsReportUseCase.execute();

        assertEquals(1, result.size());
        assertEquals(BigDecimal.ZERO, result.get(0).amount());
    }
}
