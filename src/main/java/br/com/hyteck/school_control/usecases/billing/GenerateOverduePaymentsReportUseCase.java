package br.com.hyteck.school_control.usecases.billing;

import br.com.hyteck.school_control.models.classrooms.Enrollment;
import br.com.hyteck.school_control.models.payments.Invoice;
import br.com.hyteck.school_control.models.payments.InvoiceItem;
import br.com.hyteck.school_control.models.payments.InvoiceStatus;
import br.com.hyteck.school_control.repositories.InvoiceRepository;
import br.com.hyteck.school_control.web.dtos.billing.OverduePayment;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class GenerateOverduePaymentsReportUseCase {

    private final InvoiceRepository invoiceRepository;

    public GenerateOverduePaymentsReportUseCase(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }


    @Transactional(readOnly = true)
    public List<OverduePayment> execute() {
        List<Invoice> overdueInvoices = invoiceRepository.findByStatusAndDueDateBefore(
                InvoiceStatus.OVERDUE, // Ou PENDING se PENDING e vencido = OVERDUE
                LocalDate.now()
        );

        return overdueInvoices.stream()
                .map(invoice -> {
                    Enrollment enrollment = null;
                    // Tenta obter a matrícula do primeiro item da fatura que tenha uma matrícula associada
                    if (invoice.getItems() != null && !invoice.getItems().isEmpty()) {
                        Optional<InvoiceItem> itemWithEnrollment = invoice.getItems().stream()
                                .filter(item -> item.getEnrollment() != null)
                                .findFirst();
                        if (itemWithEnrollment.isPresent()) {
                            enrollment = itemWithEnrollment.get().getEnrollment();
                        }
                    }

                    String studentName = "N/A";
                    String className = "N/A";

                    if (enrollment != null) {
                        if (enrollment.getStudent() != null) {
                            studentName = enrollment.getStudent().getName();
                        }
                        if (enrollment.getClassroom() != null) {
                            className = enrollment.getClassroom().getName();
                        }
                    }

                    String responsibleName = invoice.getResponsible() != null ? invoice.getResponsible().getName() : "N/A";

                    return new OverduePayment(
                            invoice.getId(),
                            responsibleName,
                            studentName,
                            className,
                            invoice.getAmount(),
                            invoice.getDueDate()
                    );
                })
                .collect(Collectors.toList());
    }
}