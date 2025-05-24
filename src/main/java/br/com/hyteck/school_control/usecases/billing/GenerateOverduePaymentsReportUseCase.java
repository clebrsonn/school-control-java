package br.com.hyteck.school_control.usecases.billing;

import br.com.hyteck.school_control.models.classrooms.Enrollment;
import br.com.hyteck.school_control.models.payments.Invoice;
import br.com.hyteck.school_control.models.payments.InvoiceItem;
import br.com.hyteck.school_control.models.finance.Account;
import br.com.hyteck.school_control.models.payments.*;
import br.com.hyteck.school_control.repositories.InvoiceRepository;
import br.com.hyteck.school_control.repositories.LedgerEntryRepository;
import br.com.hyteck.school_control.services.AccountService;
import br.com.hyteck.school_control.web.dtos.billing.OverduePayment;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Log4j2
public class GenerateOverduePaymentsReportUseCase {

    private final InvoiceRepository invoiceRepository;
    private final AccountService accountService;
    private final LedgerEntryRepository ledgerEntryRepository;


    @Transactional(readOnly = true)
    public List<OverduePayment> execute() {
        log.info("Generating overdue payments report for payments overdue by today: {}", LocalDate.now());
        // Invoices that are marked OVERDUE OR are PENDING and their due date has passed.
        // This logic might need refinement based on how InvoiceStatus.OVERDUE is set.
        // Assuming OVERDUE status is reliably set by another process (e.g. UpdateOverdueInvoicesUseCase).
        // If not, we might need to query PENDING invoices as well and check their due date.
        List<Invoice> overdueInvoices = invoiceRepository.findByStatusAndDueDateBefore(
                InvoiceStatus.OVERDUE,
                LocalDate.now()
        );
        log.info("Found {} invoices with OVERDUE status and due date before today.", overdueInvoices.size());


        return overdueInvoices.stream()
                .map(invoice -> {
                    Responsible responsible = invoice.getResponsible();
                    if (responsible == null || responsible.getId() == null) {
                        log.warn("Invoice ID {} is overdue but has no responsible or responsible ID. Skipping.", invoice.getId());
                        return null; // Skip this invoice or handle as appropriate
                    }

                    Account arAccount;
                    try {
                        arAccount = accountService.findOrCreateResponsibleARAccount(responsible.getId());
                        log.debug("A/R Account ID {} found/created for responsible ID {} (Invoice ID {})",
                                arAccount.getId(), responsible.getId(), invoice.getId());
                    } catch (Exception e) {
                        log.error("Error retrieving A/R account for responsible ID {} (Invoice ID {}): {}. Skipping.",
                                responsible.getId(), invoice.getId(), e.getMessage(), e);
                        return null;
                    }

                    BigDecimal currentBalanceDue = ledgerEntryRepository.getBalanceForInvoiceOnAccount(arAccount.getId(), invoice.getId());
                    log.debug("Ledger balance for overdue Invoice ID {} on A/R Account ID {} is: {}",
                            invoice.getId(), arAccount.getId(), currentBalanceDue);

                    // If balance is zero or less (e.g. overpayment/credit), it's not technically "overdue" in terms of needing payment.
                    // Depending on requirements, we might filter these out. For now, including them.
                    if (currentBalanceDue.compareTo(BigDecimal.ZERO) <= 0) {
                        log.info("Invoice ID {} for responsible ID {} has a zero or negative balance ({}). It might not be considered 'due'.",
                                invoice.getId(), responsible.getId(), currentBalanceDue);
                        // return null; // Optional: skip if not considered overdue
                    }


                    Enrollment enrollment = null;
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

                    return new OverduePayment(
                            invoice.getId(),
                            responsible.getName(), // Safe now due to earlier null check
                            studentName,
                            className,
                            currentBalanceDue, // Use ledger-derived balance
                            invoice.getDueDate()
                    );
                })
                .filter(Objects::nonNull) // Remove any nulls that resulted from errors or filtering
                .collect(Collectors.toList());
    }
}