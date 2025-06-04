package br.com.hyteck.school_control.models.payments;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class InvoiceItemTest {

    @Test
    void shouldUpdateAmountWhenNewAmountIsValid() {
        InvoiceItem item = new InvoiceItem();
        item.setAmount(new BigDecimal("100.00"));
        BigDecimal newAmount = new BigDecimal("150.00");

        item.updateAmount(newAmount);

        assertEquals(newAmount, item.getAmount());
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenNewAmountIsNull() {
        InvoiceItem item = new InvoiceItem();
        item.setAmount(new BigDecimal("100.00"));

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            item.updateAmount(null);
        });

        assertEquals("New amount cannot be null or non-positive", exception.getMessage());
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenNewAmountIsZero() {
        InvoiceItem item = new InvoiceItem();
        item.setAmount(new BigDecimal("100.00"));
        BigDecimal newAmount = BigDecimal.ZERO;

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            item.updateAmount(newAmount);
        });

        assertEquals("New amount cannot be null or non-positive", exception.getMessage());
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenNewAmountIsNegative() {
        InvoiceItem item = new InvoiceItem();
        item.setAmount(new BigDecimal("100.00"));
        BigDecimal newAmount = new BigDecimal("-50.00");

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            item.updateAmount(newAmount);
        });

        assertEquals("New amount cannot be null or non-positive", exception.getMessage());
    }
}
