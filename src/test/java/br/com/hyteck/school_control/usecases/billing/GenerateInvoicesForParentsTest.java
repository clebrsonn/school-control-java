package br.com.hyteck.school_control.usecases.billing;

import br.com.hyteck.school_control.models.classrooms.Classroom;
import br.com.hyteck.school_control.models.classrooms.Enrollment;
import br.com.hyteck.school_control.models.classrooms.Student;
// import br.com.hyteck.school_control.models.finance.Account; // Removed
// import br.com.hyteck.school_control.models.finance.AccountType; // Removed
// import br.com.hyteck.school_control.models.finance.LedgerEntryType; // Removed
import br.com.hyteck.school_control.models.payments.*;
import br.com.hyteck.school_control.repositories.DiscountRepository;
import br.com.hyteck.school_control.repositories.EnrollmentRepository;
import br.com.hyteck.school_control.repositories.InvoiceRepository;
// import br.com.hyteck.school_control.services.AccountService; // Removed
// import br.com.hyteck.school_control.services.LedgerService; // Removed
import br.com.hyteck.school_control.events.InvoiceCreatedEvent; // Changed from BatchInvoiceGeneratedEvent
import br.com.hyteck.school_control.usecases.notification.CreateNotification; // Added mock for this dependency
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDate;
// import java.time.LocalDateTime; // Removed as it was tied to LedgerService
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
    private CreateNotification createNotification; // Added mock
    @Mock
    private ApplicationEventPublisher eventPublisher;
    // Removed LedgerService and AccountService mocks

    @InjectMocks
    private GenerateInvoicesForParents generateInvoicesForParents;

    private YearMonth targetMonth;
    // Removed Account fields as they are not needed for event publishing verification
    // private Account responsibleARAccount;
    // private Account tuitionRevenueAccount;
    private Responsible responsible1;
    private Student student1;
    private Classroom classroom1;

    @BeforeEach
    void setUp() {
        targetMonth = YearMonth.of(2023, 7);

        responsible1 = createResponsible("resp1", "Responsible 1");
        student1 = createStudent("stud1", "Student 1", responsible1);
        classroom1 = createClassroom("classA", "Class A");

        // Removed Account initializations
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
        // verify(ledgerService, never()).postTransaction(any(), any(), any(), any(), any(), any(), any(), any()); // Removed
        verify(eventPublisher, never()).publishEvent(any(InvoiceCreatedEvent.class)); // Changed to InvoiceCreatedEvent
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
        // Removed AccountService mocks
        
        // Simulate the invoice being saved
        // The actual savedInvoice will be the one captured by invoiceRepository.save()
        // For event assertion, we need to ensure the event contains this invoice.
        Invoice invoiceToSave = Invoice.builder() // This is what gets built by the use case
            .responsible(responsible1)
            .referenceMonth(targetMonth)
            .issueDate(LocalDate.now()) // Or mock Clock if specific date is needed
            .dueDate(targetMonth.atDay(10)) // Simplified due date
            .status(InvoiceStatus.PENDING)
            .description("Fatura Mensalidade Julho/2023") // Example, actual may vary
            .items(new ArrayList<>())
            .build();
        invoiceToSave.addItem(InvoiceItem.builder().enrollment(enrollment).type(Types.MENSALIDADE).amount(new BigDecimal("500.00")).description("Mensalidade Julho/2023 - Aluno: Student 1").build());
        invoiceToSave.calculateAmount(); // originalAmount becomes 500.00

        // When invoiceRepository.save is called, ensure it returns an invoice with an ID.
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> {
            Invoice inv = invocation.getArgument(0);
            inv.setId("invGenerated123"); // Simulate ID assignment on save
            return inv;
        });

        generateInvoicesForParents.execute(targetMonth);

        ArgumentCaptor<Invoice> invoiceCaptor = ArgumentCaptor.forClass(Invoice.class);
        verify(invoiceRepository).save(invoiceCaptor.capture());
        Invoice capturedSavedInvoice = invoiceCaptor.getValue();
        
        assertEquals(new BigDecimal("500.00"), capturedSavedInvoice.getOriginalAmount());
        assertEquals(1, capturedSavedInvoice.getItems().size());
        assertEquals(Types.MENSALIDADE, capturedSavedInvoice.getItems().get(0).getType());

        ArgumentCaptor<InvoiceCreatedEvent> eventCaptor = ArgumentCaptor.forClass(InvoiceCreatedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture()); // Changed from BatchInvoiceGeneratedEvent
        InvoiceCreatedEvent publishedEvent = eventCaptor.getValue();
        assertNotNull(publishedEvent);
        assertEquals(capturedSavedInvoice, publishedEvent.getInvoice()); // Assert the event contains the saved invoice
        assertEquals("invGenerated123", publishedEvent.getInvoice().getId());
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
        // Removed AccountService mocks

        ArgumentCaptor<Invoice> invoiceCaptor = ArgumentCaptor.forClass(Invoice.class);
        when(invoiceRepository.save(invoiceCaptor.capture())).thenAnswer(invocation -> {
            Invoice inv = invocation.getArgument(0);
            inv.setId("invFixedDisc789"); // Set ID as if saved
            // inv.updateOriginalAmount(); // The use case calls this, so the saved mock should reflect it.
            return inv;
        });

        generateInvoicesForParents.execute(targetMonth);

        verify(invoiceRepository).save(any(Invoice.class));
        Invoice capturedSavedInvoice = invoiceCaptor.getValue();
        
        assertEquals(new BigDecimal("700.00"), capturedSavedInvoice.getOriginalAmount(), "Original amount should be NET (750 - 50).");
        assertEquals(3, capturedSavedInvoice.getItems().size(), "Should have 2 tuition items and 1 discount item.");
        
        assertTrue(capturedSavedInvoice.getItems().stream().anyMatch(item -> item.getType() == Types.MENSALIDADE && item.getAmount().compareTo(new BigDecimal("400.00")) == 0));
        assertTrue(capturedSavedInvoice.getItems().stream().anyMatch(item -> item.getType() == Types.MENSALIDADE && item.getAmount().compareTo(new BigDecimal("350.00")) == 0));
        Optional<InvoiceItem> discountItemOpt = capturedSavedInvoice.getItems().stream().filter(item -> item.getType() == Types.DESCONTO).findFirst();
        assertTrue(discountItemOpt.isPresent(), "Discount item should be present.");
        assertEquals(new BigDecimal("-50.00"), discountItemOpt.get().getAmount(), "Discount item amount should be negative.");
        assertEquals(fixedDiscountPolicy.getName(), discountItemOpt.get().getDescription());

        ArgumentCaptor<InvoiceCreatedEvent> eventCaptor = ArgumentCaptor.forClass(InvoiceCreatedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture()); // Assuming one invoice per responsible
        InvoiceCreatedEvent publishedEvent = eventCaptor.getValue();
        assertNotNull(publishedEvent);
        assertEquals(capturedSavedInvoice, publishedEvent.getInvoice());
        assertEquals("invFixedDisc789", publishedEvent.getInvoice().getId());
        // verify(ledgerService, times(1)).postTransaction(any(),any(),any(),any(),any(),any(),any(),any()); // Removed ledger check
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
        // Removed AccountService mocks

        BigDecimal grossAmount = new BigDecimal("1800.00"); // 1000 + 800
        BigDecimal discountAmount = grossAmount.multiply(new BigDecimal("0.10")); // 180.00 // This calculation happens inside the use case
        BigDecimal netAmount = grossAmount.subtract(discountAmount); // 1620.00

        ArgumentCaptor<Invoice> invoiceCaptor = ArgumentCaptor.forClass(Invoice.class);
        when(invoiceRepository.save(invoiceCaptor.capture())).thenAnswer(invocation -> {
            Invoice inv = invocation.getArgument(0);
            inv.setId("invPercDisc123");
            // inv.updateOriginalAmount(); // Done by use case
            return inv;
        });

        generateInvoicesForParents.execute(targetMonth);

        verify(invoiceRepository).save(any(Invoice.class));
        Invoice capturedSavedInvoice = invoiceCaptor.getValue();
        BigDecimal expectedNetAmount = new BigDecimal("1620.00"); // 1800 - 180

        assertEquals(expectedNetAmount, capturedSavedInvoice.getOriginalAmount(), "Original amount should be NET (1800 - 180).");
        assertEquals(3, capturedSavedInvoice.getItems().size(), "Should have 2 tuition items and 1 discount item.");
        Optional<InvoiceItem> discountItemOpt = capturedSavedInvoice.getItems().stream().filter(item -> item.getType() == Types.DESCONTO).findFirst();
        assertTrue(discountItemOpt.isPresent(), "Discount item should be present.");
        assertEquals(discountAmount.negate(), discountItemOpt.get().getAmount(), "Discount item amount should be -180.00.");

        ArgumentCaptor<InvoiceCreatedEvent> eventCaptor = ArgumentCaptor.forClass(InvoiceCreatedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        InvoiceCreatedEvent publishedEvent = eventCaptor.getValue();
        assertNotNull(publishedEvent);
        assertEquals(capturedSavedInvoice, publishedEvent.getInvoice());
        assertEquals("invPercDisc123", publishedEvent.getInvoice().getId());
        // verify(ledgerService, times(1)).postTransaction(any(),any(),any(),any(),any(),any(),any(),any()); // Removed
    }
    
    @Test
    void execute_invoiceSaveFailure_shouldThrowExceptionAndNotPublishEvent() {
        Enrollment enrollment = createEnrollment("enrollFail", student1, new BigDecimal("100"), classroom1);
        when(enrollmentRepository.findByStatus(Enrollment.Status.ACTIVE)).thenReturn(List.of(enrollment));
        when(invoiceRepository.existsByResponsibleIdAndReferenceMonthAndItems_Enrollment_Id(anyString(), any(), anyString(), any())).thenReturn(false);
        // Removed AccountService mocks

        when(invoiceRepository.save(any(Invoice.class))).thenThrow(new RuntimeException("DB save failed"));
        
        assertThrows(RuntimeException.class, () -> generateInvoicesForParents.execute(targetMonth));
        verify(invoiceRepository).save(any(Invoice.class)); // Still called
        verify(eventPublisher, never()).publishEvent(any(InvoiceCreatedEvent.class)); // But event not published
    }
}
