package br.com.hyteck.school_control.usecases.billing;

import br.com.hyteck.school_control.models.classrooms.Enrollment;
import br.com.hyteck.school_control.models.classrooms.Student;
import br.com.hyteck.school_control.models.financial.Account;
import br.com.hyteck.school_control.models.financial.AccountType;
import br.com.hyteck.school_control.models.financial.LedgerEntryType;
import br.com.hyteck.school_control.models.payments.*;
import br.com.hyteck.school_control.repositories.DiscountRepository;
import br.com.hyteck.school_control.repositories.EnrollmentRepository;
import br.com.hyteck.school_control.repositories.InvoiceRepository;
import br.com.hyteck.school_control.services.financial.AccountService;
import br.com.hyteck.school_control.events.BatchInvoiceGeneratedEvent;
import br.com.hyteck.school_control.services.financial.LedgerService;
import br.com.hyteck.school_control.usecases.notification.CreateNotification; // Will be removed from actual use case
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher; // Added
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GenerateInvoicesForParentsTest {

    @Mock
    private EnrollmentRepository enrollmentRepository;
    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private DiscountRepository discountRepository;
    // @Mock // CreateNotification will be removed from the use case's direct dependencies
    // private CreateNotification createNotificationUseCase; 
    @Mock
    private AccountService accountService;
    @Mock
    private LedgerService ledgerService;
    @Mock
    private ApplicationEventPublisher eventPublisher; // Added

    @InjectMocks
    private GenerateInvoicesForParents generateInvoicesForParents;

    private YearMonth targetMonth;
    private Responsible responsible1;
    private Student student1;
    private Enrollment enrollment1;
    private Account arAccountResp1;
    private Account tuitionRevenueAccount;
    private Account discountExpenseAccount;
    private Discount discountPolicy;

    @BeforeEach
    void setUp() {
        targetMonth = YearMonth.of(2023, 10);
        LocalDateTime fixedDateTime = LocalDateTime.of(2023, 10, 5, 10, 0, 0); // Fixed point in time for tests

        responsible1 = Responsible.builder().id("resp1").name("Responsible One").build();
        student1 = Student.builder().id("stud1").name("Student One").responsible(responsible1).build();
        enrollment1 = Enrollment.builder()
                .id("enroll1")
                .student(student1)
                .monthlyFee(new BigDecimal("300.00"))
                .status(Enrollment.Status.ACTIVE)
                .build();

        arAccountResp1 = Account.builder().id("ar1").type(AccountType.ASSET).responsible(responsible1).name("A/R - Responsible One").build();
        tuitionRevenueAccount = Account.builder().id("revAcc").type(AccountType.REVENUE).name("Tuition Revenue").build();
        discountExpenseAccount = Account.builder().id("discExpAcc").type(AccountType.EXPENSE).name("Sibling Discount").build();
        
        discountPolicy = Discount.builder().id("discPolicy1").name("Sibling Discount").type(Types.MENSALIDADE).value(new BigDecimal("50.00")).build();

        // Mock fixed time for ZoneId.of("America/Sao_Paulo") usage inside the use case
        // This is tricky, direct mocking of static ZoneId.of or LocalDateTime.now is complex.
        // It's better if the UseCase accepts a Clock or LocalDateTime as a parameter for testability,
        // or if the critical date/time is passed into the method.
        // For now, we'll assume the transactionDateTime is roughly the test execution time,
        // and dueDate calculation is deterministic based on targetMonth and this "now".
    }

    private Invoice commonInvoiceSetup(Responsible responsible, YearMonth month) {
        LocalDate issueDate = LocalDate.now(ZoneId.of("America/Sao_Paulo")); // This will use actual now, be mindful in assertions
        LocalDate dueDate = issueDate.getDayOfMonth() > 10
                ? LocalDate.of(month.getYear(), month.getMonth().plus(1), 10)
                : month.atDay(10);
        
        return Invoice.builder()
                .responsible(responsible)
                .referenceMonth(month)
                .issueDate(issueDate)
                .dueDate(dueDate)
                .status(InvoiceStatus.PENDING)
                .description("Fatura Mensal Consolidada " + month.getMonth().getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.forLanguageTag("pt-BR")) + "/" + month.getYear())
                .items(new ArrayList<>()) // Ensure items list is initialized
                .build();
    }


    @Test
    void execute_ShouldGenerateInvoiceAndPostLedgerEntries_ForSingleStudent() {
        // Arrange
        when(enrollmentRepository.findByStatus(Enrollment.Status.ACTIVE)).thenReturn(List.of(enrollment1));
        when(invoiceRepository.existsByResponsibleIdAndReferenceMonthAndItems_Enrollment_Id(
                responsible1.getId(), targetMonth, enrollment1.getId(), Types.MENSALIDADE))
                .thenReturn(false); // Not already billed
        
        Invoice expectedInvoice = commonInvoiceSetup(responsible1, targetMonth);
        expectedInvoice.addItem(InvoiceItem.builder().enrollment(enrollment1).type(Types.MENSALIDADE).description("Mensalidade").amount(enrollment1.getMonthlyFee()).build());
        expectedInvoice.updateAmount(); // amount will be 300.00
        expectedInvoice.setId("invGenId1"); // Simulate save setting an ID

        when(invoiceRepository.save(any(Invoice.class))).thenReturn(expectedInvoice);
        when(accountService.findOrCreateResponsibleARAccount(responsible1.getId())).thenReturn(arAccountResp1);
        when(accountService.findOrCreateAccount("Tuition Revenue", AccountType.REVENUE, null)).thenReturn(tuitionRevenueAccount);
        doNothing().when(ledgerService).postTransaction(any(Invoice.class), eq(null), any(Account.class), any(Account.class), any(BigDecimal.class), any(LocalDateTime.class), anyString(), any(LedgerEntryType.class));
        // doNothing().when(createNotificationUseCase).execute(anyString(), anyString(), anyString(), anyString()); // Removed
        doNothing().when(eventPublisher).publishEvent(any(BatchInvoiceGeneratedEvent.class)); // Added
        when(discountRepository.findByTypeAndValidAtBeforeToday(Types.MENSALIDADE)).thenReturn(Optional.empty()); // No discount policy


        // Act
        generateInvoicesForParents.execute(targetMonth);

        // Assert
        verify(invoiceRepository).save(any(Invoice.class));
        
        ArgumentCaptor<Invoice> invoiceCaptor = ArgumentCaptor.forClass(Invoice.class);
        verify(invoiceRepository).save(invoiceCaptor.capture());
        assertEquals(new BigDecimal("300.00"), invoiceCaptor.getValue().getAmount());

        verify(ledgerService).postTransaction(
                eq(expectedInvoice), eq(null), eq(arAccountResp1), eq(tuitionRevenueAccount),
                eq(new BigDecimal("300.00")), any(LocalDateTime.class),
                contains("Cobrança Total Mensalidades"), eq(LedgerEntryType.TUITION_FEE)
        );
        // verify(createNotificationUseCase).execute(eq("user1"), anyString(), eq("/invoices/invGenId1"), eq("NEW_MONTHLY_INVOICE")); // Removed
        verify(eventPublisher).publishEvent(any(BatchInvoiceGeneratedEvent.class)); // Added
    }

    @Test
    void execute_ShouldApplyDiscount_ForMultipleStudentsFromSameResponsible() {
        // Arrange
        Student student2 = Student.builder().id("stud2").name("Student Two").responsible(responsible1).build();
        Enrollment enrollment2 = Enrollment.builder().id("enroll2").student(student2).monthlyFee(new BigDecimal("250.00")).status(Enrollment.Status.ACTIVE).build();
        List<Enrollment> enrollments = List.of(enrollment1, enrollment2);

        when(enrollmentRepository.findByStatus(Enrollment.Status.ACTIVE)).thenReturn(enrollments);
        when(invoiceRepository.existsByResponsibleIdAndReferenceMonthAndItems_Enrollment_Id(anyString(), any(YearMonth.class), anyString(), any(Types.class)))
                .thenReturn(false); // None are pre-billed

        Invoice expectedInvoice = commonInvoiceSetup(responsible1, targetMonth);
        // Items will be added inside computeIfAbsent and loop, then amount calculated
        expectedInvoice.setId("invGenId2");

        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> {
            Invoice inv = invocation.getArgument(0);
            inv.setId(expectedInvoice.getId()); // Ensure ID is set for notification link
            // Manually add items as the use case would, to correctly calculate amount for the captor
            if(inv.getItems().isEmpty()){ // prevent adding twice if test setup is complex
                inv.addItem(InvoiceItem.builder().enrollment(enrollment1).type(Types.MENSALIDADE).description("").amount(enrollment1.getMonthlyFee()).build());
                inv.addItem(InvoiceItem.builder().enrollment(enrollment2).type(Types.MENSALIDADE).description("").amount(enrollment2.getMonthlyFee()).build());
                inv.updateAmount();
            }
            return inv;
        });

        when(accountService.findOrCreateResponsibleARAccount(responsible1.getId())).thenReturn(arAccountResp1);
        when(accountService.findOrCreateAccount("Tuition Revenue", AccountType.REVENUE, null)).thenReturn(tuitionRevenueAccount);
        when(discountRepository.findByTypeAndValidAtBeforeToday(Types.MENSALIDADE)).thenReturn(Optional.of(discountPolicy));
        when(accountService.findOrCreateAccount(discountPolicy.getName(), AccountType.EXPENSE, null)).thenReturn(discountExpenseAccount);

        doNothing().when(ledgerService).postTransaction(any(Invoice.class), eq(null), any(Account.class), any(Account.class), any(BigDecimal.class), any(LocalDateTime.class), anyString(), any(LedgerEntryType.class));
        // doNothing().when(createNotificationUseCase).execute(anyString(), anyString(), anyString(), anyString()); // Removed
        doNothing().when(eventPublisher).publishEvent(any(BatchInvoiceGeneratedEvent.class)); // Added

        // Act
        generateInvoicesForParents.execute(targetMonth);

        // Assert
        ArgumentCaptor<Invoice> invoiceCaptor = ArgumentCaptor.forClass(Invoice.class);
        verify(invoiceRepository).save(invoiceCaptor.capture());
        Invoice savedInvoice = invoiceCaptor.getValue();
        assertEquals(new BigDecimal("550.00"), savedInvoice.getAmount()); // 300 + 250

        // Verify tuition fee posting for total original amount
        verify(ledgerService).postTransaction(
                eq(savedInvoice), eq(null), eq(arAccountResp1), eq(tuitionRevenueAccount),
                eq(new BigDecimal("550.00")), any(LocalDateTime.class),
                contains("Cobrança Total Mensalidades"), eq(LedgerEntryType.TUITION_FEE)
        );

        // Verify discount posting
        verify(ledgerService).postTransaction(
                eq(savedInvoice), eq(null), eq(discountExpenseAccount), eq(arAccountResp1),
                eq(discountPolicy.getValue()), any(LocalDateTime.class), // discountPolicy.getValue() is 50.00
                contains("Desconto Multi-alunos"), eq(LedgerEntryType.DISCOUNT_APPLIED)
        );
        verify(eventPublisher).publishEvent(any(BatchInvoiceGeneratedEvent.class)); // Added
        // ArgumentCaptor<String> notificationMsgCaptor = ArgumentCaptor.forClass(String.class); // No longer need to capture notification message here
        // verify(createNotificationUseCase).execute(eq("user1"), notificationMsgCaptor.capture(), eq("/invoices/invGenId2"), eq("NEW_MONTHLY_INVOICE"));
        // assertTrue(notificationMsgCaptor.getValue().contains("R$\\u00A0500,00"), "Notification message amount mismatch. Actual: " + notificationMsgCaptor.getValue());
    }

    @Test
    void execute_ShouldSkipEnrollment_IfAlreadyBilled() {
        when(enrollmentRepository.findByStatus(Enrollment.Status.ACTIVE)).thenReturn(List.of(enrollment1));
        when(invoiceRepository.existsByResponsibleIdAndReferenceMonthAndItems_Enrollment_Id(
                responsible1.getId(), targetMonth, enrollment1.getId(), Types.MENSALIDADE))
                .thenReturn(true); // Already billed

        generateInvoicesForParents.execute(targetMonth);

        verify(invoiceRepository, never()).save(any(Invoice.class));
        verifyNoInteractions(ledgerService);
        // verifyNoInteractions(createNotificationUseCase); // Removed
        verifyNoInteractions(eventPublisher); // Added
    }
    
    @Test
    void execute_ShouldDoNothing_WhenNoActiveEnrollments() {
        when(enrollmentRepository.findByStatus(Enrollment.Status.ACTIVE)).thenReturn(Collections.emptyList());

        generateInvoicesForParents.execute(targetMonth);

        verify(invoiceRepository, never()).save(any(Invoice.class));
        verifyNoInteractions(accountService);
        verifyNoInteractions(ledgerService);
        // verifyNoInteractions(createNotificationUseCase); // Removed
        verifyNoInteractions(eventPublisher); // Added
    }
}
