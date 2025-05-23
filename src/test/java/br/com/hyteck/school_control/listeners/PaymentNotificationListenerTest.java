package br.com.hyteck.school_control.listeners;

import br.com.hyteck.school_control.events.PaymentProcessedEvent;
import br.com.hyteck.school_control.models.payments.Invoice;
import br.com.hyteck.school_control.models.payments.InvoiceStatus;
import br.com.hyteck.school_control.models.payments.PaymentStatus;
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
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentNotificationListenerTest {

    @Mock
    private CreateNotification createNotificationUseCase;

    @Mock
    private InvoiceRepository invoiceRepository;

    @InjectMocks
    private PaymentNotificationListener paymentNotificationListener;

    private UUID responsibleUserId;
    private UUID paymentId;
    private UUID invoiceId;
    private BigDecimal amountPaid;

    @BeforeEach
    void setUp() {
        responsibleUserId = UUID.randomUUID();
        paymentId = UUID.randomUUID();
        invoiceId = UUID.randomUUID();
        amountPaid = new BigDecimal("100.50");

        Invoice mockInvoice = Invoice.builder()
                                .id(invoiceId.toString())
                                .responsible(Responsible.builder().name("Responsible Test").build())
                                .build();
        when(invoiceRepository.findById(invoiceId.toString())).thenReturn(Optional.of(mockInvoice));
        doNothing().when(createNotificationUseCase).execute(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void handlePaymentProcessed_ShouldSendSuccessNotification_WhenPaymentCompleted() {
        // Arrange
        PaymentProcessedEvent event = new PaymentProcessedEvent(this, paymentId, invoiceId, amountPaid,
                PaymentStatus.COMPLETED, InvoiceStatus.PAID, responsibleUserId);

        // Act
        paymentNotificationListener.handlePaymentProcessed(event);

        // Assert
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(createNotificationUseCase).execute(
                eq(responsibleUserId.toString()),
                messageCaptor.capture(),
                eq("/invoices/" + invoiceId.toString()),
                eq("PAYMENT_SUCCESS")
        );
        assertTrue(messageCaptor.getValue().contains("processado com sucesso"));
        assertTrue(messageCaptor.getValue().contains("Status da fatura: Paga"));
    }

    @Test
    void handlePaymentProcessed_ShouldSendFailureNotification_WhenPaymentFailed() {
        // Arrange
        PaymentProcessedEvent event = new PaymentProcessedEvent(this, paymentId, invoiceId, amountPaid,
                PaymentStatus.FAILED, InvoiceStatus.PENDING, responsibleUserId); // Invoice status might remain PENDING

        // Act
        paymentNotificationListener.handlePaymentProcessed(event);

        // Assert
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(createNotificationUseCase).execute(
                eq(responsibleUserId.toString()),
                messageCaptor.capture(),
                eq("/invoices/" + invoiceId.toString()),
                eq("PAYMENT_FAILURE")
        );
        assertTrue(messageCaptor.getValue().contains("falha ao processar seu pagamento"));
    }
    
    @Test
    void handlePaymentProcessed_ShouldSendPendingNotification_WhenPaymentPendingConfirmation() {
        // Arrange
        PaymentProcessedEvent event = new PaymentProcessedEvent(this, paymentId, invoiceId, amountPaid,
                PaymentStatus.PENDING_CONFIRMATION, InvoiceStatus.PENDING, responsibleUserId);

        // Act
        paymentNotificationListener.handlePaymentProcessed(event);

        // Assert
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(createNotificationUseCase).execute(
                eq(responsibleUserId.toString()),
                messageCaptor.capture(),
                eq("/invoices/" + invoiceId.toString()),
                eq("PAYMENT_PENDING")
        );
        assertTrue(messageCaptor.getValue().contains("pendente de confirmação"));
    }


    @Test
    void handlePaymentProcessed_ShouldNotSendNotification_WhenResponsibleUserIdIsNull() {
        // Arrange
        PaymentProcessedEvent event = new PaymentProcessedEvent(this, paymentId, invoiceId, amountPaid,
                PaymentStatus.COMPLETED, InvoiceStatus.PAID, null); // Null responsibleUserId

        // Act
        paymentNotificationListener.handlePaymentProcessed(event);

        // Assert
        verifyNoInteractions(createNotificationUseCase);
    }
    
    @Test
    void handlePaymentProcessed_ShouldIncludeInvoiceDetails_WhenInvoiceFound() {
        PaymentProcessedEvent event = new PaymentProcessedEvent(this, paymentId, invoiceId, amountPaid,
                PaymentStatus.COMPLETED, InvoiceStatus.PAID, responsibleUserId);
        
        paymentNotificationListener.handlePaymentProcessed(event);
        
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(createNotificationUseCase).execute(anyString(), messageCaptor.capture(), anyString(), anyString());
        assertTrue(messageCaptor.getValue().contains("para Responsible Test"));
    }

    @Test
    void handlePaymentProcessed_ShouldUseGenericDescription_WhenInvoiceNotFound() {
        when(invoiceRepository.findById(invoiceId.toString())).thenReturn(Optional.empty());
        PaymentProcessedEvent event = new PaymentProcessedEvent(this, paymentId, invoiceId, amountPaid,
                PaymentStatus.COMPLETED, InvoiceStatus.PAID, responsibleUserId);
        
        paymentNotificationListener.handlePaymentProcessed(event);
        
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(createNotificationUseCase).execute(anyString(), messageCaptor.capture(), anyString(), anyString());
        assertTrue(messageCaptor.getValue().contains("(Fatura ID: " + invoiceId.toString().substring(0,8)));
    }

}
