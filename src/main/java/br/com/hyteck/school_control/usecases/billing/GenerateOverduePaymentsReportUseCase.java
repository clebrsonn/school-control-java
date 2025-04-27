package br.com.hyteck.school_control.usecases.billing;

import br.com.hyteck.school_control.web.dtos.billing.OverduePayment;
import br.com.hyteck.school_control.models.payments.Invoice;
import br.com.hyteck.school_control.models.payments.InvoiceStatus;
import br.com.hyteck.school_control.repositories.InvoiceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Use case para gerar um relatório de pagamentos atrasados.
 */
@Service
public class GenerateOverduePaymentsReportUseCase {

    private final InvoiceRepository invoiceRepository;

    public GenerateOverduePaymentsReportUseCase(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    /**
     * Gera um relatório de pagamentos atrasados.
     *
     * @return Uma lista de pagamentos atrasados.
     */
    @Transactional(readOnly = true)
    public List<OverduePayment> execute() {
        List<Invoice> overdueInvoices = invoiceRepository.findByStatusAndDueDateBefore(
                InvoiceStatus.OVERDUE,
                LocalDate.now()
        );

        return overdueInvoices.stream()
                .map(invoice -> new OverduePayment(
                        invoice.getId(),
                        invoice.getResponsible().getName(),
                        invoice.getEnrollment().getStudent().getName(),
                        invoice.getEnrollment().getClassroom().getName(),
                        invoice.getAmount(),
                        invoice.getDueDate()
                ))
                .collect(Collectors.toList());
    }
}
