package br.com.hyteck.school_control.usecases.billing;

import br.com.hyteck.school_control.exceptions.ResourceNotFoundException;
import br.com.hyteck.school_control.models.payments.Invoice;
import br.com.hyteck.school_control.models.payments.Payment;
import br.com.hyteck.school_control.models.payments.PaymentMethod;
import br.com.hyteck.school_control.models.payments.PaymentStatus;
import br.com.hyteck.school_control.repositories.InvoiceRepository;
import br.com.hyteck.school_control.repositories.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
public class ProcessPaymentUseCase {

    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;

    public ProcessPaymentUseCase(InvoiceRepository invoiceRepository, PaymentRepository paymentRepository) {
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
    }

    @Transactional
    public Payment execute(String invoiceId, BigDecimal amount, PaymentMethod paymentMethod) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));

//        if (amount.compareTo(invoice.getTotalExpenses()) != 0) {
//            throw new IllegalArgumentException("Payment amount does not match invoice total");
//        }

        Payment payment = Payment.builder()
                .amountPaid(amount)
                .paymentDate(LocalDate.now().atStartOfDay())
                .paymentMethod(paymentMethod)
                .invoice(invoice)
                .status(PaymentStatus.COMPLETED)
                .build();

        return paymentRepository.save(payment);
    }
}
