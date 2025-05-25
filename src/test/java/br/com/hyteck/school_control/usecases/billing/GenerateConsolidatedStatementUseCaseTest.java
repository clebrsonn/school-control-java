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
import br.com.hyteck.school_control.repositories.LedgerEntryRepository; // Corrected path if needed
import br.com.hyteck.school_control.repositories.ResponsibleRepository;
import br.com.hyteck.school_control.services.AccountService; // Corrected path if needed
import br.com.hyteck.school_control.web.dtos.billing.ConsolidatedStatement;
import br.com.hyteck.school_control.web.dtos.billing.StatementLineItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;


@ExtendWith(MockitoExtension.class)
class GenerateConsolidatedStatementUseCaseTest {

    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private ResponsibleRepository responsibleRepository;
    @Mock
    private AccountService accountService; // Added mock
    @Mock
    private LedgerEntryRepository ledgerEntryRepository; // Added mock

    @InjectMocks
    private GenerateConsolidatedStatementUseCase generateConsolidatedStatementUseCase;

    private YearMonth targetMonth;
    private Responsible responsible1;
    private Account arAccountResp1;
    private Student student1;
    private Classroom classroom1;


    @BeforeEach
    void setUp() {
        targetMonth = YearMonth.of(2023, 9); // September 2023
        responsible1 = Responsible.builder().id("resp1").name("Responsible One").build();
        arAccountResp1 = Account.builder().id("ar1").type(AccountType.ASSET).responsible(responsible1).name("A/R - Responsible One").build();
        student1 = Student.builder().id("stud1").name("Student One").responsible(responsible1).build();
        classroom1 = Classroom.builder().id("classA").name("Class A").build();
    }

    private Invoice createInvoice(String id, Responsible responsible, InvoiceStatus status, BigDecimal originalAmount, List<InvoiceItem> items) {
        Invoice invoice = Invoice.builder()
                .id(id)
                .responsible(responsible)
                .referenceMonth(targetMonth)
                .status(status)
                .originalAmount(originalAmount)
                .dueDate(targetMonth.atDay(10))
                .issueDate(targetMonth.atDay(1).minusMonths(1))
                .items(new ArrayList<>()) 
                .description("Invoice " + id)
                .build();
        items.forEach(item -> { // Ensure items are correctly associated
            item.setInvoice(invoice); // Set bidirectional relationship
            invoice.getItems().add(item);
        });
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

    private Enrollment createEnrollment(String id, Student student, Classroom classroom) {
        return Enrollment.builder()
            .id(id)
            .student(student)
            .classroom(classroom)
            .monthlyFee(new BigDecimal("100")) 
            .status(Enrollment.Status.ACTIVE)
            .build();
    }


    // Tests for execute(String responsibleId, YearMonth referenceMonth)
    @Test
    void executeByResponsible_noResponsibleFound_shouldReturnEmptyOptional() {
        // Arrange
        when(responsibleRepository.findById("nonExistentResp")).thenReturn(Optional.empty());

        // Act
        Optional<ConsolidatedStatement> result = generateConsolidatedStatementUseCase.execute("nonExistentResp", targetMonth);

        // Assert
        assertTrue(result.isEmpty());
        verify(invoiceRepository, never()).findPendingInvoicesByResponsibleAndMonth(any(), any(), anyList());
        verify(accountService, never()).findOrCreateResponsibleARAccount(any());
        verify(ledgerEntryRepository, never()).getBalanceForInvoiceOnAccount(any(), any());
    }

    @Test
    void executeByResponsible_noPendingOrOverdueInvoices_shouldReturnEmptyOptional() {
        // Arrange
        List<InvoiceStatus> expectedStatuses = Arrays.asList(InvoiceStatus.PENDING, InvoiceStatus.OVERDUE);
        when(responsibleRepository.findById(responsible1.getId())).thenReturn(Optional.of(responsible1));
        when(invoiceRepository.findPendingInvoicesByResponsibleAndMonth(
                eq(responsible1.getId()), eq(targetMonth), eq(expectedStatuses)))
                .thenReturn(Collections.emptyList());

        // Act
        Optional<ConsolidatedStatement> result = generateConsolidatedStatementUseCase.execute(responsible1.getId(), targetMonth);

        // Assert
        assertTrue(result.isEmpty());
        verify(accountService, never()).findOrCreateResponsibleARAccount(any()); // Should not be called if no invoices
    }

    @Test
    void executeByResponsible_invoicesExist_shouldReturnConsolidatedStatementWithLedgerBalances() {
        // Arrange
        Enrollment enrollment1 = createEnrollment("enroll1", student1, classroom1);
        InvoiceItem item1 = createInvoiceItem("item1", enrollment1, new BigDecimal("300.00"), "Tuition Fee Stud1");
        Invoice invoice1 = createInvoice("inv1", responsible1, InvoiceStatus.PENDING, new BigDecimal("300.00"), List.of(item1));
        
        Student student2 = Student.builder().id("stud2").name("Student Two").responsible(responsible1).build();
        Enrollment enrollment2 = createEnrollment("enroll2", student2, classroom1);
        InvoiceItem item2 = createInvoiceItem("item2", enrollment2, new BigDecimal("250.00"), "Activity Fee Stud2");
        Invoice invoice2 = createInvoice("inv2", responsible1, InvoiceStatus.OVERDUE, new BigDecimal("250.00"), List.of(item2));
        
        List<Invoice> invoices = List.of(invoice1, invoice2);
        List<InvoiceStatus> expectedStatuses = Arrays.asList(InvoiceStatus.PENDING, InvoiceStatus.OVERDUE);

        when(responsibleRepository.findById(responsible1.getId())).thenReturn(Optional.of(responsible1));
        when(invoiceRepository.findPendingInvoicesByResponsibleAndMonth(responsible1.getId(), targetMonth, expectedStatuses))
                .thenReturn(invoices);
        when(accountService.findOrCreateResponsibleARAccount(responsible1.getId())).thenReturn(arAccountResp1);

        when(ledgerEntryRepository.getBalanceForInvoiceOnAccount(arAccountResp1.getId(), invoice1.getId()))
                .thenReturn(new BigDecimal("280.00")); 
        when(ledgerEntryRepository.getBalanceForInvoiceOnAccount(arAccountResp1.getId(), invoice2.getId()))
                .thenReturn(new BigDecimal("200.00")); 

        // Act
        Optional<ConsolidatedStatement> resultOpt = generateConsolidatedStatementUseCase.execute(responsible1.getId(), targetMonth);

        // Assert
        assertTrue(resultOpt.isPresent());
        ConsolidatedStatement result = resultOpt.get();

        assertEquals(responsible1.getId(), result.responsibleId());
        assertEquals(responsible1.getName(), result.responsibleName());
        assertEquals(targetMonth, result.referenceMonth());
        assertEquals(2, result.items().size(), "Should have two line items");

        assertEquals(new BigDecimal("480.00"), result.totalAmountDue(), "Total due should be sum of ledger balances (280+200)");

        StatementLineItem lineItem1 = result.items().stream().filter(li -> li.invoiceId().equals("inv1")).findFirst().orElse(null);
        assertNotNull(lineItem1, "Line item for inv1 should exist");
        assertEquals(new BigDecimal("280.00"), lineItem1.amount(), "Amount for inv1 should be its ledger balance");
        assertEquals(student1.getName(), lineItem1.studentName());

        StatementLineItem lineItem2 = result.items().stream().filter(li -> li.invoiceId().equals("inv2")).findFirst().orElse(null);
        assertNotNull(lineItem2, "Line item for inv2 should exist");
        assertEquals(new BigDecimal("200.00"), lineItem2.amount(), "Amount for inv2 should be its ledger balance");
        assertEquals(student2.getName(), lineItem2.studentName());
        
        verify(responsibleRepository).findById(responsible1.getId());
        verify(invoiceRepository).findPendingInvoicesByResponsibleAndMonth(responsible1.getId(), targetMonth, expectedStatuses);
        verify(accountService).findOrCreateResponsibleARAccount(responsible1.getId());
        verify(ledgerEntryRepository).getBalanceForInvoiceOnAccount(arAccountResp1.getId(), invoice1.getId());
        verify(ledgerEntryRepository).getBalanceForInvoiceOnAccount(arAccountResp1.getId(), invoice2.getId());
    }

    // Tests for execute(YearMonth referenceMonth)
    @Test
    void executeByMonth_noPendingOrOverdueInvoices_shouldReturnEmptyList() {
        // Arrange
        List<InvoiceStatus> expectedStatuses = Arrays.asList(InvoiceStatus.PENDING, InvoiceStatus.OVERDUE);
        when(invoiceRepository.findPendingInvoicesByMonth(eq(targetMonth), eq(expectedStatuses)))
                .thenReturn(Collections.emptyList());

        // Act
        List<ConsolidatedStatement> result = generateConsolidatedStatementUseCase.execute(targetMonth);

        // Assert
        assertTrue(result.isEmpty());
        verify(accountService, never()).findOrCreateResponsibleARAccount(any());
    }

    @Test
    void executeByMonth_invoicesExistForMultipleResponsibles_shouldReturnListOfConsolidatedStatements() {
        // Arrange
        // Responsible 1
        Enrollment enrollmentR1S1 = createEnrollment("enrollR1S1", student1, classroom1); // student1 is resp1's
        InvoiceItem itemR1S1 = createInvoiceItem("itemR1S1", enrollmentR1S1, new BigDecimal("100.00"), "Tuition R1S1");
        Invoice invoiceR1 = createInvoice("invR1", responsible1, InvoiceStatus.PENDING, new BigDecimal("100.00"), List.of(itemR1S1));

        // Responsible 2
        Responsible responsible2 = Responsible.builder().id("resp2").name("Responsible Two").build();
        Account arAccountResp2 = Account.builder().id("ar2").type(AccountType.ASSET).responsible(responsible2).name("A/R - Responsible Two").build();
        Student studentR2S1 = Student.builder().id("studR2S1").name("Student R2S1").responsible(responsible2).build();
        Enrollment enrollmentR2S1 = createEnrollment("enrollR2S1", studentR2S1, classroom1);
        InvoiceItem itemR2S1 = createInvoiceItem("itemR2S1", enrollmentR2S1, new BigDecimal("150.00"), "Tuition R2S1");
        Invoice invoiceR2 = createInvoice("invR2", responsible2, InvoiceStatus.OVERDUE, new BigDecimal("150.00"), List.of(itemR2S1));
        
        List<Invoice> allInvoices = List.of(invoiceR1, invoiceR2);
        List<InvoiceStatus> expectedStatuses = Arrays.asList(InvoiceStatus.PENDING, InvoiceStatus.OVERDUE);

        when(invoiceRepository.findPendingInvoicesByMonth(targetMonth, expectedStatuses)).thenReturn(allInvoices);

        when(accountService.findOrCreateResponsibleARAccount(responsible1.getId())).thenReturn(arAccountResp1);
        when(accountService.findOrCreateResponsibleARAccount(responsible2.getId())).thenReturn(arAccountResp2);

        when(ledgerEntryRepository.getBalanceForInvoiceOnAccount(arAccountResp1.getId(), invoiceR1.getId()))
                .thenReturn(new BigDecimal("90.00")); 
        when(ledgerEntryRepository.getBalanceForInvoiceOnAccount(arAccountResp2.getId(), invoiceR2.getId()))
                .thenReturn(new BigDecimal("140.00")); 

        // Act
        List<ConsolidatedStatement> results = generateConsolidatedStatementUseCase.execute(targetMonth);

        // Assert
        assertEquals(2, results.size(), "Should be one statement per responsible");

        ConsolidatedStatement statementR1 = results.stream().filter(s -> s.responsibleId().equals(responsible1.getId())).findFirst().orElse(null);
        assertNotNull(statementR1, "Statement for responsible1 should exist");
        assertEquals(new BigDecimal("90.00"), statementR1.totalAmountDue());
        assertEquals(1, statementR1.items().size());
        assertEquals(new BigDecimal("90.00"), statementR1.items().get(0).amount());
        assertEquals(student1.getName(), statementR1.items().get(0).studentName());


        ConsolidatedStatement statementR2 = results.stream().filter(s -> s.responsibleId().equals(responsible2.getId())).findFirst().orElse(null);
        assertNotNull(statementR2, "Statement for responsible2 should exist");
        assertEquals(new BigDecimal("140.00"), statementR2.totalAmountDue());
        assertEquals(1, statementR2.items().size());
        assertEquals(new BigDecimal("140.00"), statementR2.items().get(0).amount());
        assertEquals(studentR2S1.getName(), statementR2.items().get(0).studentName());
        
        verify(invoiceRepository).findPendingInvoicesByMonth(targetMonth, expectedStatuses);
        verify(accountService).findOrCreateResponsibleARAccount(responsible1.getId());
        verify(accountService).findOrCreateResponsibleARAccount(responsible2.getId());
        verify(ledgerEntryRepository).getBalanceForInvoiceOnAccount(arAccountResp1.getId(), invoiceR1.getId());
        verify(ledgerEntryRepository).getBalanceForInvoiceOnAccount(arAccountResp2.getId(), invoiceR2.getId());
    }
    
    @Test
    void executeByMonth_invoiceWithNoResponsible_shouldBeSkippedAndLogged() {
        // Arrange
        // student1 is linked to responsible1, but we'll set invoice's responsible to null
        Enrollment enrollmentNoResp = createEnrollment("enrollNoResp", student1, classroom1); 
        InvoiceItem itemNoResp = createInvoiceItem("itemNoResp", enrollmentNoResp, new BigDecimal("50.00"), "Fee No Resp");
        Invoice invoiceNoResponsible = createInvoice("invNoResp", null, InvoiceStatus.PENDING, new BigDecimal("50.00"), List.of(itemNoResp));
        invoiceNoResponsible.setResponsible(null); // Ensure responsible is null
        
        List<Invoice> allInvoices = List.of(invoiceNoResponsible);
        List<InvoiceStatus> expectedStatuses = Arrays.asList(InvoiceStatus.PENDING, InvoiceStatus.OVERDUE);

        when(invoiceRepository.findPendingInvoicesByMonth(targetMonth, expectedStatuses)).thenReturn(allInvoices);
        
        // Act
        List<ConsolidatedStatement> results = generateConsolidatedStatementUseCase.execute(targetMonth);
        
        // Assert
        assertTrue(results.isEmpty(), "List should be empty as the only invoice has no responsible");
        verify(accountService, never()).findOrCreateResponsibleARAccount(any());
        verify(ledgerEntryRepository, never()).getBalanceForInvoiceOnAccount(any(), any());
    }

    @Test
    void executeByResponsible_invoiceItemWithNoEnrollment_shouldHandleGracefully() {
        // Arrange
        InvoiceItem itemNoEnroll = createInvoiceItem("itemNoEnroll", null, new BigDecimal("50.00"), "Misc Fee");
        Invoice invoiceWithItemNoEnroll = createInvoice("invNoEnroll", responsible1, InvoiceStatus.PENDING, new BigDecimal("50.00"), List.of(itemNoEnroll));
        
        List<Invoice> invoices = List.of(invoiceWithItemNoEnroll);
        List<InvoiceStatus> expectedStatuses = Arrays.asList(InvoiceStatus.PENDING, InvoiceStatus.OVERDUE);

        when(responsibleRepository.findById(responsible1.getId())).thenReturn(Optional.of(responsible1));
        when(invoiceRepository.findPendingInvoicesByResponsibleAndMonth(responsible1.getId(), targetMonth, expectedStatuses))
                .thenReturn(invoices);
        when(accountService.findOrCreateResponsibleARAccount(responsible1.getId())).thenReturn(arAccountResp1);
        when(ledgerEntryRepository.getBalanceForInvoiceOnAccount(arAccountResp1.getId(), invoiceWithItemNoEnroll.getId()))
                .thenReturn(new BigDecimal("40.00"));

        // Act
        Optional<ConsolidatedStatement> resultOpt = generateConsolidatedStatementUseCase.execute(responsible1.getId(), targetMonth);

        // Assert
        assertTrue(resultOpt.isPresent());
        ConsolidatedStatement result = resultOpt.get();
        assertEquals(1, result.items().size());
        StatementLineItem lineItem = result.items().get(0);
        assertEquals("N/A", lineItem.studentName(), "Student name should be N/A if enrollment is null");
        assertEquals(new BigDecimal("40.00"), lineItem.amount());
    }
}
