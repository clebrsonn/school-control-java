package br.com.hyteck.school_control.usecases.billing;

import br.com.hyteck.school_control.models.classrooms.Classroom;
import br.com.hyteck.school_control.models.classrooms.Enrollment;
import br.com.hyteck.school_control.models.classrooms.Student;
import br.com.hyteck.school_control.models.finance.Account;
import br.com.hyteck.school_control.models.finance.AccountType;
import br.com.hyteck.school_control.models.finance.LedgerEntryType;
import br.com.hyteck.school_control.models.payments.*;
import br.com.hyteck.school_control.repositories.DiscountRepository;
import br.com.hyteck.school_control.repositories.EnrollmentRepository;
import br.com.hyteck.school_control.repositories.InvoiceRepository;
import br.com.hyteck.school_control.services.AccountService;
import br.com.hyteck.school_control.services.LedgerService;
import br.com.hyteck.school_control.events.BatchInvoiceGeneratedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GenerateInvoicesForParentsTest {

    @Mock
    private DiscountRepository discountRepository;
    @Mock
    private EnrollmentRepository enrollmentRepository;
    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private AccountService accountService;
    @Mock
    private LedgerService ledgerService;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private GenerateInvoicesForParents generateInvoicesForParents;

    private YearMonth targetMonth;
    private Account responsibleARAccount;
    private Account tuitionRevenueAccount;
    // discountExpenseAccount is no longer directly used in ledger postings for itemized discounts
    // private Account discountExpenseAccount; 
    private Responsible responsible1;
    private Student student1;
    private Classroom classroom1;

    @BeforeEach
    void setUp() {
        targetMonth = YearMonth.of(2023, 7); 

        responsible1 = createResponsible("resp1", "Responsible 1");
        student1 = createStudent("stud1", "Student 1", responsible1);
        classroom1 = createClassroom("classA", "Class A");

        responsibleARAccount = Account.builder().id("arAccountId").name("Responsible A/R").type(AccountType.ASSET).build();
        tuitionRevenueAccount = Account.builder().id("tuitionRevenueAccountId").name("Tuition Revenue").type(AccountType.REVENUE).build();
        // discountExpenseAccount = Account.builder().id("discountExpenseAccountId").name("Discount Expense").type(AccountType.EXPENSE).build();
    }

    private Responsible createResponsible(String id, String name) {
        return Responsible.builder().id(id).name(name).user(br.com.hyteck.school_control.models.auth.User.builder().id(id).build()).build();
    }

    private Student createStudent(String id, String name, Responsible responsible) {
        return Student.builder().id(id).name(name).responsible(responsible).build();
    }
    
    private Classroom createClassroom(String id, String name) {
        return Classroom.builder().id(id).name(name).build();
    }

    private Enrollment createEnrollment(String id, Student student, BigDecimal monthlyFee, Classroom classroom) {
        return Enrollment.builder()
                .id(id)
                .student(student)
                .monthlyFee(monthlyFee)
                .status(Enrollment.Status.ACTIVE)
                .classroom(classroom)
                .build();
    }

    // --- Edge Case Tests (Largely Unchanged) ---
    @Test
    void execute_noActiveEnrollments_shouldNotCreateInvoicesOrPostTransactions() {
        when(enrollmentRepository.findByStatus(Enrollment.Status.ACTIVE)).thenReturn(Collections.emptyList());
        generateInvoicesForParents.execute(targetMonth);
        verify(invoiceRepository, never()).save(any(Invoice.class));
        verify(ledgerService, never()).postTransaction(any(), any(), any(), any(), any(), any(), any(), any());
        verify(eventPublisher, never()).publishEvent(any(BatchInvoiceGeneratedEvent.class));
    }

    @Test
    void execute_enrollmentWithZeroMonthlyFee_shouldSkipEnrollment() {
        Enrollment enrollment = createEnrollment("enroll1", student1, BigDecimal.ZERO, classroom1); 
        when(enrollmentRepository.findByStatus(Enrollment.Status.ACTIVE)).thenReturn(List.of(enrollment));
        generateInvoicesForParents.execute(targetMonth);
        verify(invoiceRepository, never()).save(any(Invoice.class));
    }
    
    @Test
    void execute_enrollmentWithNullMonthlyFee_shouldSkipEnrollment() {
        Enrollment enrollment = createEnrollment("enroll1", student1, null, classroom1);
        when(enrollmentRepository.findByStatus(Enrollment.Status.ACTIVE)).thenReturn(List.of(enrollment));
        generateInvoicesForParents.execute(targetMonth);
        verify(invoiceRepository, never()).save(any(Invoice.class));
    }

    @Test
    void execute_enrollmentWithNoResponsible_shouldSkipEnrollment() {
        Student studentNoResponsible = createStudent("studNR", "Student No Responsible", null);
        Enrollment enrollment = createEnrollment("enrollNR", studentNoResponsible, new BigDecimal("100"), classroom1);
        when(enrollmentRepository.findByStatus(Enrollment.Status.ACTIVE)).thenReturn(List.of(enrollment));
        generateInvoicesForParents.execute(targetMonth);
        verify(invoiceRepository, never()).save(any(Invoice.class));
    }

    @Test
    void execute_invoiceAlreadyBilled_shouldSkipEnrollment() {
        Enrollment enrollment = createEnrollment("enrollBilled", student1, new BigDecimal("100"), classroom1);
        when(enrollmentRepository.findByStatus(Enrollment.Status.ACTIVE)).thenReturn(List.of(enrollment));
        when(invoiceRepository.existsByResponsibleIdAndReferenceMonthAndItems_Enrollment_Id(
                eq(responsible1.getId()), eq(targetMonth), eq(enrollment.getId()), eq(Types.MENSALIDADE)))
                .thenReturn(true);
        generateInvoicesForParents.execute(targetMonth);
        verify(invoiceRepository, never()).save(any(Invoice.class));
    }

    // --- Core Logic Tests (Updated for Net OriginalAmount and Itemized Discounts) ---
    @Test
    void execute_successfulInvoiceGeneration_singleEnrollment_noDiscount() {
        Enrollment enrollment = createEnrollment("enrollSingle", student1, new BigDecimal("500.00"), classroom1);
        when(enrollmentRepository.findByStatus(Enrollment.Status.ACTIVE)).thenReturn(List.of(enrollment));
        when(invoiceRepository.existsByResponsibleIdAndReferenceMonthAndItems_Enrollment_Id(anyString(), any(), anyString(), any())).thenReturn(false); 
        when(accountService.findOrCreateAccount(eq(GenerateInvoicesForParents.TUITION_REVENUE_ACCOUNT_NAME), eq(AccountType.REVENUE),isNull())).thenReturn(tuitionRevenueAccount);
        when(accountService.findOrCreateResponsibleARAccount(eq(responsible1.getId()))).thenReturn(responsibleARAccount);
        
        // Simulate the invoice being saved
        Invoice savedInvoice = Invoice.builder().id("invGenerated123").responsible(responsible1).referenceMonth(targetMonth).originalAmount(new BigDecimal("500.00")).items(new ArrayList<>()).build();
        savedInvoice.addItem(InvoiceItem.builder().enrollment(enrollment).type(Types.MENSALIDADE).amount(new BigDecimal("500.00")).build());
        // No discount item, so originalAmount remains 500.00 after updateOriginalAmount() inside use case

        ArgumentCaptor<Invoice> invoiceCaptor = ArgumentCaptor.forClass(Invoice.class);
        when(invoiceRepository.save(invoiceCaptor.capture())).thenReturn(savedInvoice); // Return the "saved" invoice with ID

        generateInvoicesForParents.execute(targetMonth);

        verify(invoiceRepository).save(any(Invoice.class));
        Invoice capturedInvoice = invoiceCaptor.getValue();
        assertEquals(new BigDecimal("500.00"), capturedInvoice.getOriginalAmount(), "Original amount should be the gross fee as no discount applies.");
        assertEquals(1, capturedInvoice.getItems().size());
        assertEquals(Types.MENSALIDADE, capturedInvoice.getItems().get(0).getType());

        verify(ledgerService).postTransaction(
                eq(savedInvoice), isNull(), eq(responsibleARAccount), eq(tuitionRevenueAccount),
                eq(new BigDecimal("500.00")), // Net amount (which is gross here)
                any(LocalDateTime.class), anyString(), eq(LedgerEntryType.TUITION_FEE)
        );
        verify(eventPublisher).publishEvent(any(BatchInvoiceGeneratedEvent.class));
    }

    @Test
    void execute_successfulInvoiceGeneration_multipleEnrollments_sameResponsible_withFixedDiscount() {
        Student student2 = createStudent("studD2", "Student D2", responsible1);
        Enrollment enrollment1 = createEnrollment("enrollD1", student1, new BigDecimal("400.00"), classroom1);
        Enrollment enrollment2 = createEnrollment("enrollD2", student2, new BigDecimal("350.00"), createClassroom("classB", "Class B"));
        List<Enrollment> enrollments = List.of(enrollment1, enrollment2);

        Discount fixedDiscountPolicy = Discount.builder()
            .id("discFixed1").name("Fixed Sibling Discount").type(Types.MENSALIDADE)
            .fixedValue(new BigDecimal("50.00")).validAt(LocalDate.now().minusDays(1)).build();

        when(enrollmentRepository.findByStatus(Enrollment.Status.ACTIVE)).thenReturn(enrollments);
        when(invoiceRepository.existsByResponsibleIdAndReferenceMonthAndItems_Enrollment_Id(anyString(), any(), anyString(), any())).thenReturn(false);
        when(discountRepository.findByTypeAndValidAtBeforeToday(Types.MENSALIDADE)).thenReturn(Optional.of(fixedDiscountPolicy));
        when(accountService.findOrCreateAccount(eq(GenerateInvoicesForParents.TUITION_REVENUE_ACCOUNT_NAME), eq(AccountType.REVENUE),isNull())).thenReturn(tuitionRevenueAccount);
        when(accountService.findOrCreateResponsibleARAccount(eq(responsible1.getId()))).thenReturn(responsibleARAccount);

        // Simulate the invoice being saved
        BigDecimal expectedNetOgirinalAmount = new BigDecimal("750.00").subtract(new BigDecimal("50.00")); // 700.00
        Invoice savedInvoice = Invoice.builder().id("invFixedDisc789").responsible(responsible1).referenceMonth(targetMonth).originalAmount(expectedNetOgirinalAmount).items(new ArrayList<>()).build();
          // Items will be added by the use case logic before save is called.
          // The originalAmount in savedInvoice mock should reflect the *final* net amount.

        ArgumentCaptor<Invoice> invoiceCaptor = ArgumentCaptor.forClass(Invoice.class);
        when(invoiceRepository.save(invoiceCaptor.capture())).thenAnswer(invocation -> {
            Invoice inv = invocation.getArgument(0);
            inv.setId("invFixedDisc789"); // Set ID as if saved
            return inv; // Return the same captured invoice, now with an ID
        });


        generateInvoicesForParents.execute(targetMonth);

        verify(invoiceRepository).save(any(Invoice.class));
        Invoice capturedInvoice = invoiceCaptor.getValue();
        
        assertEquals(new BigDecimal("700.00"), capturedInvoice.getOriginalAmount(), "Original amount should be NET (750 - 50).");
        assertEquals(3, capturedInvoice.getItems().size(), "Should have 2 tuition items and 1 discount item.");
        
        assertTrue(capturedInvoice.getItems().stream().anyMatch(item -> item.getType() == Types.MENSALIDADE && item.getAmount().compareTo(new BigDecimal("400.00")) == 0));
        assertTrue(capturedInvoice.getItems().stream().anyMatch(item -> item.getType() == Types.MENSALIDADE && item.getAmount().compareTo(new BigDecimal("350.00")) == 0));
        Optional<InvoiceItem> discountItemOpt = capturedInvoice.getItems().stream().filter(item -> item.getType() == Types.DESCONTO).findFirst();
        assertTrue(discountItemOpt.isPresent(), "Discount item should be present.");
        assertEquals(new BigDecimal("-50.00"), discountItemOpt.get().getAmount(), "Discount item amount should be negative.");
        assertEquals(fixedDiscountPolicy.getName(), discountItemOpt.get().getDescription());


        verify(ledgerService).postTransaction(
                any(Invoice.class), // or eq(capturedInvoice) if ID is set before ledgerService call
                isNull(), 
                eq(responsibleARAccount), 
                eq(tuitionRevenueAccount),
                eq(new BigDecimal("700.00")), // NET amount
                any(LocalDateTime.class), 
                contains("(Líquido de Descontos Itemizados)"), // Verify description reflects net posting
                eq(LedgerEntryType.TUITION_FEE)
        );
        // Ensure no separate discount ledger posting for this itemized discount
        verify(ledgerService, times(1)).postTransaction(any(),any(),any(),any(),any(),any(),any(),any());
        verify(eventPublisher).publishEvent(any(BatchInvoiceGeneratedEvent.class));
    }
    
    @Test
    void execute_successfulInvoiceGeneration_withPercentageDiscount() {
        Student student2 = createStudent("studPD2", "Student PD2", responsible1);
        Enrollment enrollment1 = createEnrollment("enrollPD1", student1, new BigDecimal("1000.00"), classroom1);
        Enrollment enrollment2 = createEnrollment("enrollPD2", student2, new BigDecimal("800.00"), createClassroom("classB", "Class B"));
        List<Enrollment> enrollments = List.of(enrollment1, enrollment2);

        Discount percentageDiscountPolicy = Discount.builder()
            .id("discPerc1").name("Percentage Sibling Discount").type(Types.MENSALIDADE)
            .percentage(new BigDecimal("10.00")).validAt(LocalDate.now().minusDays(1)).build(); // 10%

        when(enrollmentRepository.findByStatus(Enrollment.Status.ACTIVE)).thenReturn(enrollments);
        when(invoiceRepository.existsByResponsibleIdAndReferenceMonthAndItems_Enrollment_Id(anyString(), any(), anyString(), any())).thenReturn(false);
        when(discountRepository.findByTypeAndValidAtBeforeToday(Types.MENSALIDADE)).thenReturn(Optional.of(percentageDiscountPolicy));
        when(accountService.findOrCreateAccount(eq(GenerateInvoicesForParents.TUITION_REVENUE_ACCOUNT_NAME), eq(AccountType.REVENUE), isNull())).thenReturn(tuitionRevenueAccount);
        when(accountService.findOrCreateResponsibleARAccount(eq(responsible1.getId()))).thenReturn(responsibleARAccount);

        BigDecimal grossAmount = new BigDecimal("1800.00"); // 1000 + 800
        BigDecimal discountAmount = grossAmount.multiply(new BigDecimal("0.10")); // 180.00
        BigDecimal netAmount = grossAmount.subtract(discountAmount); // 1620.00

        ArgumentCaptor<Invoice> invoiceCaptor = ArgumentCaptor.forClass(Invoice.class);
         when(invoiceRepository.save(invoiceCaptor.capture())).thenAnswer(invocation -> {
            Invoice inv = invocation.getArgument(0);
            inv.setId("invPercDisc123"); 
            return inv;
        });

        generateInvoicesForParents.execute(targetMonth);

        verify(invoiceRepository).save(any(Invoice.class));
        Invoice capturedInvoice = invoiceCaptor.getValue();

        assertEquals(netAmount, capturedInvoice.getOriginalAmount(), "Original amount should be NET (1800 - 180).");
        assertEquals(3, capturedInvoice.getItems().size(), "Should have 2 tuition items and 1 discount item.");
        Optional<InvoiceItem> discountItemOpt = capturedInvoice.getItems().stream().filter(item -> item.getType() == Types.DESCONTO).findFirst();
        assertTrue(discountItemOpt.isPresent(), "Discount item should be present.");
        assertEquals(discountAmount.negate(), discountItemOpt.get().getAmount(), "Discount item amount should be -180.00.");

        verify(ledgerService).postTransaction(
                any(Invoice.class), isNull(), eq(responsibleARAccount), eq(tuitionRevenueAccount),
                eq(netAmount), // NET amount
                any(LocalDateTime.class), contains("(Líquido de Descontos Itemizados)"), eq(LedgerEntryType.TUITION_FEE)
        );
        verify(ledgerService, times(1)).postTransaction(any(),any(),any(),any(),any(),any(),any(),any());
        verify(eventPublisher).publishEvent(any(BatchInvoiceGeneratedEvent.class));
    }
    
    @Test
    void execute_ledgerTransactionFailure_shouldThrowExceptionAndNotPublishEvent() {
        Enrollment enrollment = createEnrollment("enrollFail", student1, new BigDecimal("100"), classroom1);
        when(enrollmentRepository.findByStatus(Enrollment.Status.ACTIVE)).thenReturn(List.of(enrollment));
        when(invoiceRepository.existsByResponsibleIdAndReferenceMonthAndItems_Enrollment_Id(anyString(), any(), anyString(), any())).thenReturn(false);
        when(accountService.findOrCreateAccount(eq(GenerateInvoicesForParents.TUITION_REVENUE_ACCOUNT_NAME), eq(AccountType.REVENUE), isNull())).thenReturn(tuitionRevenueAccount);
        when(accountService.findOrCreateResponsibleARAccount(eq(responsible1.getId()))).thenReturn(responsibleARAccount);

        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> {
            Invoice inv = invocation.getArgument(0);
            inv.setId("invFailLedger123");
            return inv;
        });
        
        doThrow(new RuntimeException("Ledger posting failed")).when(ledgerService).postTransaction(any(), isNull(), any(), any(), any(), any(), anyString(), eq(LedgerEntryType.TUITION_FEE));

        assertThrows(RuntimeException.class, () -> generateInvoicesForParents.execute(targetMonth));
        verify(invoiceRepository).save(any(Invoice.class));
        verify(eventPublisher, never()).publishEvent(any(BatchInvoiceGeneratedEvent.class));
    }
}
