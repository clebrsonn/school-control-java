package br.com.hyteck.school_control.listeners;

import br.com.hyteck.school_control.events.InvoiceCreatedEvent;
import br.com.hyteck.school_control.models.classrooms.Enrollment;
import br.com.hyteck.school_control.models.classrooms.Student;
import br.com.hyteck.school_control.models.financial.Account;
import br.com.hyteck.school_control.models.financial.AccountType;
import br.com.hyteck.school_control.models.financial.LedgerEntryType;
import br.com.hyteck.school_control.models.payments.Invoice;
import br.com.hyteck.school_control.models.payments.InvoiceItem;
import br.com.hyteck.school_control.models.payments.Responsible;
import br.com.hyteck.school_control.models.payments.Types;
import br.com.hyteck.school_control.services.financial.AccountService;
import br.com.hyteck.school_control.services.financial.LedgerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class LedgerEntryCreationListenerTest {

    @Mock
    private LedgerService ledgerService;

    @Mock
    private AccountService accountService;

    @InjectMocks
    private LedgerEntryCreationListener listener;

    private Responsible responsible;
    private Account arAccount;
    private Account enrollmentRevenueAccount;
    private Account tuitionRevenueAccount;
    private Invoice.Builder baseInvoiceBuilder;

    @Captor
    private ArgumentCaptor<LocalDateTime> dateCaptor;

    @BeforeEach
    void setUp() {
        responsible = Responsible.builder().id(UUID.randomUUID().toString()).name("Test Responsible").build();
        arAccount = Account.builder().id(UUID.randomUUID().toString()).name("A/R Test").type(AccountType.ASSET).build();
        enrollmentRevenueAccount = Account.builder().id(UUID.randomUUID().toString()).name("Enrollment Fee Revenue").type(AccountType.REVENUE).build();
        tuitionRevenueAccount = Account.builder().id(UUID.randomUUID().toString()).name("Tuition Revenue").type(AccountType.REVENUE).build();

        baseInvoiceBuilder = Invoice.builder()
                .id(UUID.randomUUID().toString())
                .responsible(responsible)
                .amount(BigDecimal.valueOf(100))
                .referenceMonth(YearMonth.now());
    }

    @Test
    void handleInvoiceCreatedEvent_forEnrollmentFee_shouldPostCorrectLedgerTransaction() {
        // Arrange
        Invoice invoice = baseInvoiceBuilder.build();
        
        Student student = Student.builder().id(UUID.randomUUID().toString()).name("Test Student").build();
        Enrollment enrollment = Enrollment.builder().id(UUID.randomUUID().toString()).student(student).build();
        InvoiceItem item = InvoiceItem.builder().type(Types.MATRICULA).amount(BigDecimal.valueOf(100)).enrollment(enrollment).build();
        invoice.setItems(Collections.singletonList(item));

        when(accountService.findOrCreateResponsibleARAccount(responsible.getId())).thenReturn(arAccount);
        when(accountService.findOrCreateAccount("Enrollment Fee Revenue", AccountType.REVENUE, null)).thenReturn(enrollmentRevenueAccount);

        InvoiceCreatedEvent event = new InvoiceCreatedEvent(this, invoice);

        // Act
        listener.handleInvoiceCreatedEvent(event);

        // Assert
        verify(ledgerService).postTransaction(
                eq(invoice),
                isNull(),
                eq(arAccount),
                eq(enrollmentRevenueAccount),
                eq(BigDecimal.valueOf(100)),
                dateCaptor.capture(),
                contains("Enrollment fee for student: Test Student"),
                eq(LedgerEntryType.ENROLLMENT_FEE_CHARGED)
        );
        assertTrue(dateCaptor.getValue().isBefore(LocalDateTime.now().plusSeconds(2)) && dateCaptor.getValue().isAfter(LocalDateTime.now().minusSeconds(5)));
    }

    @Test
    void handleInvoiceCreatedEvent_forMonthlyFee_shouldPostCorrectLedgerTransaction() {
        // Arrange
        Invoice invoice = baseInvoiceBuilder.amount(BigDecimal.valueOf(250)).build();
        InvoiceItem item = InvoiceItem.builder().type(Types.MENSALIDADE).amount(BigDecimal.valueOf(250)).build();
        invoice.setItems(Collections.singletonList(item));

        when(accountService.findOrCreateResponsibleARAccount(responsible.getId())).thenReturn(arAccount);
        when(accountService.findOrCreateAccount("Tuition Revenue", AccountType.REVENUE, null)).thenReturn(tuitionRevenueAccount);

        InvoiceCreatedEvent event = new InvoiceCreatedEvent(this, invoice);

        // Act
        listener.handleInvoiceCreatedEvent(event);

        // Assert
        verify(ledgerService).postTransaction(
                eq(invoice),
                isNull(),
                eq(arAccount),
                eq(tuitionRevenueAccount),
                eq(BigDecimal.valueOf(250)),
                dateCaptor.capture(),
                contains("Monthly tuition fee - Invoice: " + invoice.getId()),
                eq(LedgerEntryType.TUITION_FEE)
        );
        assertTrue(dateCaptor.getValue().isBefore(LocalDateTime.now().plusSeconds(2)) && dateCaptor.getValue().isAfter(LocalDateTime.now().minusSeconds(5)));
    }
    
    @Test
    void handleInvoiceCreatedEvent_forMixedItemsPrioritizesEnrollment_shouldPostEnrollmentLedgerTransaction() {
        // Arrange
        Invoice invoice = baseInvoiceBuilder.amount(BigDecimal.valueOf(350)).build(); // Total amount
    
        Student student = Student.builder().id(UUID.randomUUID().toString()).name("Mixed Student").build();
        Enrollment enrollment = Enrollment.builder().id(UUID.randomUUID().toString()).student(student).build();
    
        InvoiceItem enrollmentItem = InvoiceItem.builder().type(Types.MATRICULA).amount(BigDecimal.valueOf(100)).enrollment(enrollment).build();
        InvoiceItem monthlyItem = InvoiceItem.builder().type(Types.MENSALIDADE).amount(BigDecimal.valueOf(250)).enrollment(enrollment).build(); // Assuming same enrollment for simplicity
        invoice.setItems(List.of(enrollmentItem, monthlyItem)); // Order might matter if stream().findFirst() was used, but anyMatch doesn't care
    
        when(accountService.findOrCreateResponsibleARAccount(responsible.getId())).thenReturn(arAccount);
        when(accountService.findOrCreateAccount("Enrollment Fee Revenue", AccountType.REVENUE, null)).thenReturn(enrollmentRevenueAccount);
        // Note: We don't expect findOrCreateAccount for "Tuition Revenue" to be called if MATRICULA is prioritized
    
        InvoiceCreatedEvent event = new InvoiceCreatedEvent(this, invoice);
    
        // Act
        listener.handleInvoiceCreatedEvent(event);
    
        // Assert
        verify(ledgerService).postTransaction(
                eq(invoice),
                isNull(),
                eq(arAccount),
                eq(enrollmentRevenueAccount), // Expecting Enrollment specific account
                eq(BigDecimal.valueOf(350)),    // The total invoice amount
                dateCaptor.capture(),
                contains("Enrollment fee for student: Mixed Student"), // Description for enrollment
                eq(LedgerEntryType.ENROLLMENT_FEE_CHARGED)       // Type for enrollment
        );
        assertTrue(dateCaptor.getValue().isBefore(LocalDateTime.now().plusSeconds(2)) && dateCaptor.getValue().isAfter(LocalDateTime.now().minusSeconds(5)));
        verify(accountService, never()).findOrCreateAccount("Tuition Revenue", AccountType.REVENUE, null);
    }


    @Test
    void handleInvoiceCreatedEvent_forOtherItemType_shouldUseFallback() {
        // Arrange
        Invoice invoice = baseInvoiceBuilder.amount(BigDecimal.valueOf(50)).build();
        InvoiceItem item = InvoiceItem.builder().type(Types.DESCONTO).amount(BigDecimal.valueOf(50).negate()).build(); // Example of non-primary type
        invoice.setItems(Collections.singletonList(item));

        when(accountService.findOrCreateResponsibleARAccount(responsible.getId())).thenReturn(arAccount);
        when(accountService.findOrCreateAccount("Tuition Revenue", AccountType.REVENUE, null)).thenReturn(tuitionRevenueAccount); // Fallback account

        InvoiceCreatedEvent event = new InvoiceCreatedEvent(this, invoice);

        // Act
        listener.handleInvoiceCreatedEvent(event);

        // Assert
        verify(ledgerService).postTransaction(
                eq(invoice),
                isNull(),
                eq(arAccount),
                eq(tuitionRevenueAccount), // Fallback account
                eq(BigDecimal.valueOf(50)),
                dateCaptor.capture(),
                contains("Invoice charges - Invoice: " + invoice.getId()), // Fallback description
                eq(LedgerEntryType.TUITION_FEE) // Fallback type
        );
        assertTrue(dateCaptor.getValue().isBefore(LocalDateTime.now().plusSeconds(2)) && dateCaptor.getValue().isAfter(LocalDateTime.now().minusSeconds(5)));
    }
    
    @Test
    void handleInvoiceCreatedEvent_forEnrollmentFee_studentNameRetrievalFailsGracefully() {
        // Arrange
        Invoice invoice = baseInvoiceBuilder.build();
        // Enrollment or student is null or name is null
        Enrollment enrollmentWithNullStudent = Enrollment.builder().id(UUID.randomUUID().toString()).student(null).build();
        InvoiceItem item = InvoiceItem.builder().type(Types.MATRICULA).amount(BigDecimal.valueOf(100)).enrollment(enrollmentWithNullStudent).build();
        invoice.setItems(Collections.singletonList(item));
    
        when(accountService.findOrCreateResponsibleARAccount(responsible.getId())).thenReturn(arAccount);
        when(accountService.findOrCreateAccount("Enrollment Fee Revenue", AccountType.REVENUE, null)).thenReturn(enrollmentRevenueAccount);
    
        InvoiceCreatedEvent event = new InvoiceCreatedEvent(this, invoice);
    
        // Act
        listener.handleInvoiceCreatedEvent(event);
    
        // Assert
        verify(ledgerService).postTransaction(
                any(Invoice.class),
                isNull(),
                any(Account.class),
                any(Account.class),
                any(BigDecimal.class),
                any(LocalDateTime.class),
                contains("Enrollment fee for student: N/A"), // Expect "N/A"
                eq(LedgerEntryType.ENROLLMENT_FEE_CHARGED)
        );
    }


    @Test
    void handleInvoiceCreatedEvent_withNoItems_shouldNotPostTransaction() {
        // Arrange
        Invoice invoice = baseInvoiceBuilder.build();
        invoice.setItems(Collections.emptyList()); // No items

        InvoiceCreatedEvent event = new InvoiceCreatedEvent(this, invoice);

        // Act
        listener.handleInvoiceCreatedEvent(event);

        // Assert
        verifyNoInteractions(ledgerService); // ledgerService should not be called
        verify(accountService, never()).findOrCreateResponsibleARAccount(any()); // accountService methods related to posting also shouldn't be called
        verify(accountService, never()).findOrCreateAccount(anyString(), any(AccountType.class), any());
    }

    @Test
    void handleInvoiceCreatedEvent_withNoResponsible_shouldNotPostTransaction() {
        // Arrange
        Invoice invoice = baseInvoiceBuilder.responsible(null).build(); // No responsible
        InvoiceItem item = InvoiceItem.builder().type(Types.MENSALIDADE).amount(BigDecimal.valueOf(100)).build();
        invoice.setItems(Collections.singletonList(item));


        InvoiceCreatedEvent event = new InvoiceCreatedEvent(this, invoice);

        // Act
        listener.handleInvoiceCreatedEvent(event);

        // Assert
        verifyNoInteractions(ledgerService);
        verify(accountService, never()).findOrCreateResponsibleARAccount(any());
        verify(accountService, never()).findOrCreateAccount(anyString(), any(AccountType.class), any());
    }
}
