package br.com.hyteck.school_control.listeners;

import br.com.hyteck.school_control.events.InvoiceStatusChangedEvent;
import br.com.hyteck.school_control.models.classrooms.Enrollment;
import br.com.hyteck.school_control.models.classrooms.Student;
import br.com.hyteck.school_control.models.payments.Invoice;
import br.com.hyteck.school_control.models.payments.InvoiceItem;
import br.com.hyteck.school_control.models.payments.InvoiceStatus;
import br.com.hyteck.school_control.models.payments.Responsible;
import br.com.hyteck.school_control.repositories.InvoiceRepository;
import br.com.hyteck.school_control.usecases.notification.CreateNotification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoiceStatusChangeNotificationListenerTest {

    @Mock
    private CreateNotification createNotificationUseCase;

    @Mock
    private InvoiceRepository invoiceRepository;

    @InjectMocks
    private InvoiceStatusChangeNotificationListener invoiceStatusChangeNotificationListener;

    private UUID responsibleUserId;
    private UUID invoiceId;
    private Invoice testInvoice;

    @BeforeEach
    void setUp() {
        responsibleUserId = UUID.randomUUID();
        invoiceId = UUID.randomUUID();

        Responsible resp = Responsible.builder().id("respId").name("Responsible Test").build();
        Student stud = Student.builder().id("studTest").name("Student Test").build();
        Enrollment enr = Enrollment.builder().id("enrTest").student(stud).build();
        InvoiceItem item = InvoiceItem.builder().id("itemTest").enrollment(enr).amount(new BigDecimal("150.00")).build();
        List<InvoiceItem> items = new ArrayList<>();
        items.add(item);

        testInvoice = Invoice.builder()
                .id(invoiceId.toString())
                .responsible(resp)
                .originalAmount(new BigDecimal("150.00"))
                .dueDate(LocalDate.now().plusDays(1))
                .referenceMonth(YearMonth.now())
                .items(items)
                .build();
        
        when(invoiceRepository.findById(invoiceId.toString())).thenReturn(Optional.of(testInvoice));
        doNothing().when(createNotificationUseCase).execute(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void handleInvoiceStatusChanged_ToPaid_ShouldSendPaidNotification() {
        // Arrange
        InvoiceStatusChangedEvent event = new InvoiceStatusChangedEvent(this, invoiceId,
                InvoiceStatus.PENDING, InvoiceStatus.PAID, responsibleUserId);

        // Act
        invoiceStatusChangeNotificationListener.handleInvoiceStatusChanged(event);

        // Assert
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(createNotificationUseCase).execute(
                eq(responsibleUserId.toString()),
                messageCaptor.capture(),
                eq("/invoices/" + invoiceId.toString()),
                eq("INVOICE_PAID")
        );
        assertTrue(messageCaptor.getValue().contains("atualizado para PAGA"));
    }

    @Test
    void handleInvoiceStatusChanged_ToOverdue_ShouldSendOverdueNotification() {
        // Arrange
        testInvoice.setDueDate(LocalDate.now().minusDays(1)); // Ensure it's actually overdue for message context
        InvoiceStatusChangedEvent event = new InvoiceStatusChangedEvent(this, invoiceId,
                InvoiceStatus.PENDING, InvoiceStatus.OVERDUE, responsibleUserId);
        
        // Act
        invoiceStatusChangeNotificationListener.handleInvoiceStatusChanged(event);

        // Assert
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(createNotificationUseCase).execute(
                eq(responsibleUserId.toString()),
                messageCaptor.capture(),
                eq("/invoices/" + invoiceId.toString()),
                eq("INVOICE_OVERDUE")
        );
        assertTrue(messageCaptor.getValue().contains("atualizado para VENCIDA"));
        assertTrue(messageCaptor.getValue().contains("R$\u00A0150,00")); // Check for formatted amount
    }
    
    @Test
    void handleInvoiceStatusChanged_FromOverdueToPending_ShouldSendPendingUpdateNotification() {
        testInvoice.setDueDate(LocalDate.now().plusDays(5)); // Due date extended
        InvoiceStatusChangedEvent event = new InvoiceStatusChangedEvent(this, invoiceId,
                InvoiceStatus.OVERDUE, InvoiceStatus.PENDING, responsibleUserId);
        
        invoiceStatusChangeNotificationListener.handleInvoiceStatusChanged(event);
        
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(createNotificationUseCase).execute(
                eq(responsibleUserId.toString()),
                messageCaptor.capture(),
                eq("/invoices/" + invoiceId.toString()),
                eq("INVOICE_PENDING_UPDATE")
        );
        assertTrue(messageCaptor.getValue().contains("atualizado para PENDENTE. Novo vencimento:"));
    }
    
    @Test
    void handleInvoiceStatusChanged_FromPaidToPending_ShouldSendPaymentReversedNotification() {
        InvoiceStatusChangedEvent event = new InvoiceStatusChangedEvent(this, invoiceId,
                InvoiceStatus.PAID, InvoiceStatus.PENDING, responsibleUserId);
        
        invoiceStatusChangeNotificationListener.handleInvoiceStatusChanged(event);
        
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(createNotificationUseCase).execute(
                eq(responsibleUserId.toString()),
                messageCaptor.capture(),
                eq("/invoices/" + invoiceId.toString()),
                eq("INVOICE_PAYMENT_REVERSED")
        );
        assertTrue(messageCaptor.getValue().contains("mudou de PAGA para PENDENTE"));
    }


    @Test
    void handleInvoiceStatusChanged_ShouldNotSendNotification_WhenResponsibleUserIdIsNull() {
        // Arrange
        InvoiceStatusChangedEvent event = new InvoiceStatusChangedEvent(this, invoiceId,
                InvoiceStatus.PENDING, InvoiceStatus.PAID, null); // Null responsibleUserId

        // Act
        invoiceStatusChangeNotificationListener.handleInvoiceStatusChanged(event);

        // Assert
        verifyNoInteractions(createNotificationUseCase);
    }

    @Test
    void handleInvoiceStatusChanged_ShouldNotSendNotification_WhenInvoiceNotFound() {
        // Arrange
        UUID nonExistentInvoiceId = UUID.randomUUID();
        InvoiceStatusChangedEvent event = new InvoiceStatusChangedEvent(this, nonExistentInvoiceId,
                InvoiceStatus.PENDING, InvoiceStatus.PAID, responsibleUserId);
        when(invoiceRepository.findById(nonExistentInvoiceId.toString())).thenReturn(Optional.empty());

        // Act
        invoiceStatusChangeNotificationListener.handleInvoiceStatusChanged(event);

        // Assert
        verify(invoiceRepository).findById(nonExistentInvoiceId.toString());
        verifyNoInteractions(createNotificationUseCase);
    }
    
    @Test
    void handleInvoiceStatusChanged_ShouldNotSend_ForUnhandledStatusTransition() {
        InvoiceStatusChangedEvent event = new InvoiceStatusChangedEvent(this, invoiceId,
                InvoiceStatus.PENDING, InvoiceStatus.CANCELLED, responsibleUserId); // Example of unhandled specific message

        invoiceStatusChangeNotificationListener.handleInvoiceStatusChanged(event);

        verifyNoInteractions(createNotificationUseCase); // No specific message for PENDING -> CANCELLED
    }

}
