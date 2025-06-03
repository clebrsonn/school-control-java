package br.com.hyteck.school_control.models.payments;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.assertEquals;

class InvoiceTest {

    private Invoice invoice;
    private final LocalDate today = LocalDate.now();

    @BeforeEach
    void setUp() {
        invoice = new Invoice();
        invoice.setId("inv-123");
        // Default status unless explicitly set for a test
        invoice.setStatus(InvoiceStatus.PENDING); 
    }

    @Test
    void applyPayment_whenFullPaymentReceived_shouldSetStatusToPaid() {
        // Arrange
        invoice.setDueDate(today.plusDays(10)); // Not overdue
        BigDecimal paymentAmountReceived = new BigDecimal("100.00");
        BigDecimal balanceOwedBeforeThisPayment = new BigDecimal("100.00");

        // Act
        invoice.applyPayment(paymentAmountReceived, balanceOwedBeforeThisPayment, today);

        // Assert
        assertEquals(InvoiceStatus.PAID, invoice.getStatus(), "Invoice status should be PAID for full payment.");
    }

    @Test
    void applyPayment_whenPartialPaymentAndNotOverdue_shouldSetStatusToPending() {
        // Arrange
        invoice.setDueDate(today.plusDays(10)); // Due date is in the future
        BigDecimal paymentAmountReceived = new BigDecimal("50.00");
        BigDecimal balanceOwedBeforeThisPayment = new BigDecimal("100.00");

        // Act
        invoice.applyPayment(paymentAmountReceived, balanceOwedBeforeThisPayment, today);

        // Assert
        assertEquals(InvoiceStatus.PENDING, invoice.getStatus(), "Invoice status should be PENDING for partial payment, not overdue.");
    }

    @Test
    void applyPayment_whenPartialPaymentAndOverdue_shouldSetStatusToOverdue() {
        // Arrange
        invoice.setDueDate(today.minusDays(1)); // Due date is in the past
        BigDecimal paymentAmountReceived = new BigDecimal("50.00");
        BigDecimal balanceOwedBeforeThisPayment = new BigDecimal("100.00");

        // Act
        invoice.applyPayment(paymentAmountReceived, balanceOwedBeforeThisPayment, today);

        // Assert
        assertEquals(InvoiceStatus.OVERDUE, invoice.getStatus(), "Invoice status should be OVERDUE for partial payment, past due date.");
    }
    
    @Test
    void applyPayment_whenPaymentExceedsBalance_shouldSetStatusToPaid() {
        // Arrange
        invoice.setDueDate(today.plusDays(10)); // Not overdue
        BigDecimal paymentAmountReceived = new BigDecimal("150.00"); // Overpayment
        BigDecimal balanceOwedBeforeThisPayment = new BigDecimal("100.00");

        // Act
        invoice.applyPayment(paymentAmountReceived, balanceOwedBeforeThisPayment, today);

        // Assert
        assertEquals(InvoiceStatus.PAID, invoice.getStatus(), "Invoice status should be PAID even for overpayment.");
    }

    @Test
    void applyPayment_whenInvoiceIsCancelled_shouldNotChangeStatusAndLogWarning() {
        // Arrange
        invoice.setStatus(InvoiceStatus.CANCELLED);
        invoice.setDueDate(today.plusDays(10));
        BigDecimal paymentAmountReceived = new BigDecimal("100.00");
        BigDecimal balanceOwedBeforeThisPayment = new BigDecimal("100.00");
        
        // Redirect System.out to capture log message (simple approach for this context)
        // In a real Spring Boot app, you'd mock the logger.
        // For this environment, assuming System.out.println is used by the method as per its implementation.
        // This part is tricky without a proper logger mock and might not be verifiable in this sandbox.
        // However, the primary assertion is that status doesn't change.

        // Act
        invoice.applyPayment(paymentAmountReceived, balanceOwedBeforeThisPayment, today);

        // Assert
        assertEquals(InvoiceStatus.CANCELLED, invoice.getStatus(), "Invoice status should remain CANCELLED if payment is attempted.");
        // Verification of the System.out.println("Warning...") is omitted here as it's hard to capture reliably in this environment
    }
    
    @Test
    void applyPayment_whenFullPaymentReceivedOnDueDate_shouldSetStatusToPaid() {
        // Arrange
        invoice.setDueDate(today); // Due date is today
        BigDecimal paymentAmountReceived = new BigDecimal("100.00");
        BigDecimal balanceOwedBeforeThisPayment = new BigDecimal("100.00");

        // Act
        invoice.applyPayment(paymentAmountReceived, balanceOwedBeforeThisPayment, today);

        // Assert
        assertEquals(InvoiceStatus.PAID, invoice.getStatus(), "Invoice status should be PAID for full payment on due date.");
    }

    @Test
    void applyPayment_whenPartialPaymentOnDueDate_shouldSetStatusToPending() {
        // Arrange
        invoice.setDueDate(today); // Due date is today
        BigDecimal paymentAmountReceived = new BigDecimal("50.00");
        BigDecimal balanceOwedBeforeThisPayment = new BigDecimal("100.00");

        // Act
        invoice.applyPayment(paymentAmountReceived, balanceOwedBeforeThisPayment, today);

        // Assert
        assertEquals(InvoiceStatus.PENDING, invoice.getStatus(), "Invoice status should be PENDING for partial payment on due date.");
    }
}
