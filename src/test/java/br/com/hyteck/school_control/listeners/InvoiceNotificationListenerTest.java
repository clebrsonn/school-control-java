package br.com.hyteck.school_control.listeners;

import br.com.hyteck.school_control.events.BatchInvoiceGeneratedEvent;
import br.com.hyteck.school_control.models.classrooms.Enrollment;
import br.com.hyteck.school_control.models.classrooms.Student;
import br.com.hyteck.school_control.models.payments.Invoice;
import br.com.hyteck.school_control.models.payments.InvoiceItem;
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
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoiceNotificationListenerTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private CreateNotification createNotificationUseCase;

    @InjectMocks
    private InvoiceNotificationListener invoiceNotificationListener;

    private BatchInvoiceGeneratedEvent event;
    private UUID responsibleUserId;
    private UUID responsibleId;
    private UUID invoiceId1;
    private Invoice representativeInvoice;
    private YearMonth targetMonth;

    @BeforeEach
    void setUp() {
        responsibleUserId = UUID.randomUUID();
        responsibleId = UUID.randomUUID();
        invoiceId1 = UUID.randomUUID();
        targetMonth = YearMonth.of(2023, 11);

        // Setup representativeInvoice
        Responsible resp = Responsible.builder().id(responsibleId.toString()).name("Responsible Test").build();
        Student stud = Student.builder().id("studTest1").name("Student Test").build();
        Enrollment enr = Enrollment.builder().id("enrTest1").student(stud).build();
        InvoiceItem item = InvoiceItem.builder().id("itemTest1").enrollment(enr).amount(new BigDecimal("250.00")).build();
        
        List<InvoiceItem> items = new ArrayList<>();
        items.add(item);

        representativeInvoice = Invoice.builder()
                .id(invoiceId1.toString())
                .responsible(resp)
                .originalAmount(new BigDecimal("250.00"))
                .dueDate(LocalDate.now().plusMonths(1))
                .referenceMonth(targetMonth)
                .items(items) // Make sure items are initialized
                .build();
    }

    @Test
    void handleBatchInvoiceGenerated_ShouldSendNotification_WhenEventIsValid() {
        // Arrange
        List<UUID> invoiceIds = List.of(invoiceId1);
        event = new BatchInvoiceGeneratedEvent(this, invoiceIds, targetMonth, responsibleId, responsibleUserId);

        when(invoiceRepository.findById(invoiceId1.toString())).thenReturn(Optional.of(representativeInvoice));
        doNothing().when(createNotificationUseCase).execute(anyString(), anyString(), anyString(), anyString());

        // Act
        invoiceNotificationListener.handleBatchInvoiceGenerated(event);

        // Assert
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(createNotificationUseCase).execute(
                eq(responsibleUserId.toString()),
                messageCaptor.capture(),
                eq("/invoices/" + invoiceId1.toString()),
                eq("NEW_MONTHLY_INVOICE")
        );
        
        String capturedMessage = messageCaptor.getValue();
        assertTrue(capturedMessage.contains("Nova(s) fatura(s) de mensalidade (Ref: 11/2023)"));
        assertTrue(capturedMessage.contains("Student Test")); // Check if student name is in message
        assertTrue(capturedMessage.contains("R$\u00A0250,00")); // Check for formatted amount

    }

    @Test
    void handleBatchInvoiceGenerated_ShouldNotSendNotification_WhenNoInvoiceIds() {
        // Arrange
        event = new BatchInvoiceGeneratedEvent(this, Collections.emptyList(), targetMonth, responsibleId, responsibleUserId);

        // Act
        invoiceNotificationListener.handleBatchInvoiceGenerated(event);

        // Assert
        verifyNoInteractions(invoiceRepository);
        verifyNoInteractions(createNotificationUseCase);
    }

    @Test
    void handleBatchInvoiceGenerated_ShouldNotSendNotification_WhenRepresentativeInvoiceNotFound() {
        // Arrange
        List<UUID> invoiceIds = List.of(invoiceId1);
        event = new BatchInvoiceGeneratedEvent(this, invoiceIds, targetMonth, responsibleId, responsibleUserId);
        when(invoiceRepository.findById(invoiceId1.toString())).thenReturn(Optional.empty());

        // Act
        invoiceNotificationListener.handleBatchInvoiceGenerated(event);

        // Assert
        verify(invoiceRepository).findById(invoiceId1.toString());
        verifyNoInteractions(createNotificationUseCase);
    }
    
    @Test
    void handleBatchInvoiceGenerated_ShouldHandleExceptionFromCreateNotification() {
        // Arrange
        List<UUID> invoiceIds = List.of(invoiceId1);
        event = new BatchInvoiceGeneratedEvent(this, invoiceIds, targetMonth, responsibleId, responsibleUserId);

        when(invoiceRepository.findById(invoiceId1.toString())).thenReturn(Optional.of(representativeInvoice));
        doThrow(new RuntimeException("Test email sending error")).when(createNotificationUseCase)
            .execute(anyString(), anyString(), anyString(), anyString());

        // Act
        // As the method is @Async and catches exceptions, the test itself won't see the exception
        invoiceNotificationListener.handleBatchInvoiceGenerated(event);

        // Assert
        // Verify it was called, even if it threw an exception internally (logged by listener)
        verify(createNotificationUseCase).execute(
            eq(responsibleUserId.toString()), anyString(), anyString(), anyString()
        );
        // Log verification would be ideal here if a testable logger was injected into the listener
    }
}
