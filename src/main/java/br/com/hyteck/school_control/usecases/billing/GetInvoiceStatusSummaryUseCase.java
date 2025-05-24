package br.com.hyteck.school_control.usecases.billing;

import br.com.hyteck.school_control.models.finance.Account;
import br.com.hyteck.school_control.models.payments.Invoice;
import br.com.hyteck.school_control.models.payments.InvoiceStatus;
import br.com.hyteck.school_control.models.payments.Payment;
import br.com.hyteck.school_control.models.payments.Responsible;
import br.com.hyteck.school_control.repositories.InvoiceRepository;
import br.com.hyteck.school_control.repositories.LedgerEntryRepository;
import br.com.hyteck.school_control.repositories.PaymentRepository;
import br.com.hyteck.school_control.services.AccountService;
import br.com.hyteck.school_control.web.dtos.billing.InvoiceBasicInfo;
import br.com.hyteck.school_control.web.dtos.billing.InvoiceStatusSummaryDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Log4j2
public class GetInvoiceStatusSummaryUseCase {

    private final InvoiceRepository invoiceRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final AccountService accountService;
    private final PaymentRepository paymentRepository; // Needed for paid invoice analysis

    @Transactional(readOnly = true)
    public InvoiceStatusSummaryDto executeOverallSummary() {
        log.info("Executing overall invoice status summary.");

        List<InvoiceBasicInfo> pendingInvoicesList = new ArrayList<>();
        BigDecimal totalPendingBalance = BigDecimal.ZERO;
        long totalPendingInvoices = 0;

        List<InvoiceBasicInfo> overdueInvoicesList = new ArrayList<>();
        BigDecimal totalOverdueBalance = BigDecimal.ZERO;
        long totalOverdueInvoices = 0;

        // Process PENDING invoices
        List<Invoice> pendingInvoices = invoiceRepository.findByStatus(InvoiceStatus.PENDING);
        log.info("Found {} PENDING invoices.", pendingInvoices.size());
        for (Invoice invoice : pendingInvoices) {
            InvoiceBasicInfo basicInfo = createInvoiceBasicInfo(invoice);
            if (basicInfo != null) {
                pendingInvoicesList.add(basicInfo);
                totalPendingBalance = totalPendingBalance.add(basicInfo.currentBalanceDue());
            }
        }
        totalPendingInvoices = pendingInvoicesList.size();

        // Process OVERDUE invoices
        // Assuming OVERDUE status is correctly set by another process.
        // If not, this might need to fetch PENDING invoices past their due date.
        List<Invoice> overdueInvoices = invoiceRepository.findByStatus(InvoiceStatus.OVERDUE);
        log.info("Found {} OVERDUE invoices.", overdueInvoices.size());
        for (Invoice invoice : overdueInvoices) {
            InvoiceBasicInfo basicInfo = createInvoiceBasicInfo(invoice);
            if (basicInfo != null) {
                overdueInvoicesList.add(basicInfo);
                totalOverdueBalance = totalOverdueBalance.add(basicInfo.currentBalanceDue());
            }
        }
        totalOverdueInvoices = overdueInvoicesList.size();

        return InvoiceStatusSummaryDto.builder()
                .totalPendingInvoices(totalPendingInvoices)
                .totalPendingBalance(totalPendingBalance)
                .pendingInvoicesList(pendingInvoicesList)
                .totalOverdueInvoices(totalOverdueInvoices)
                .totalOverdueBalance(totalOverdueBalance)
                .overdueInvoicesList(overdueInvoicesList)
                // Paid stats are not part of this specific summary method
                .totalPaidOnTimeInPeriod(0)
                .totalPaidLateInPeriod(0)
                .build();
    }


    @Transactional(readOnly = true)
    public InvoiceStatusSummaryDto executePaidInvoiceSummaryForPeriod(YearMonth period) {
        log.info("Executing paid invoice summary for period: {}", period);

        LocalDate startDate = period.atDay(1);
        LocalDate endDate = period.atEndOfMonth();

        // Fetch payments made within the period using LocalDate as per PaymentRepository method.
        List<Payment> paymentsInPeriod = paymentRepository.findByPaymentDateBetween(startDate, endDate);
        log.info("Found {} payments within the period {} to {}.", paymentsInPeriod.size(), startDate, endDate);

        long paidOnTime = 0;
        long paidLate = 0;

        for (Payment payment : paymentsInPeriod) {
            Invoice invoice = payment.getInvoice();
            if (invoice == null) {
                log.warn("Payment ID {} has no associated invoice. Skipping.", payment.getId());
                continue;
            }

            // Ensure the invoice status is PAID. A payment might exist but invoice status update failed.
            // Or, a payment might be partial if such logic exists (though current system treats payments as full).
            if (invoice.getStatus() != InvoiceStatus.PAID) {
                log.warn("Invoice ID {} associated with Payment ID {} is not in PAID status (Status: {}). " +
                        "Considering it for paid summary based on payment existence, but this might indicate inconsistency.",
                        invoice.getId(), payment.getId(), invoice.getStatus());
                // Depending on strictness, one might skip these or include them.
            }

            LocalDate dueDate = invoice.getDueDate();
            LocalDateTime paymentDate = payment.getPaymentDate();

            if (dueDate == null || paymentDate == null) {
                log.warn("Invoice ID {} or Payment ID {} has null due date or payment date. Cannot determine on-time/late status.",
                        invoice.getId(), payment.getId());
                continue;
            }

            if (paymentDate.toLocalDate().isAfter(dueDate)) {
                paidLate++;
            } else {
                paidOnTime++;
            }
        }

        log.info("Paid on time: {}, Paid late: {} for period {}", paidOnTime, paidLate, period);

        return InvoiceStatusSummaryDto.builder()
                .totalPaidOnTimeInPeriod(paidOnTime)
                .totalPaidLateInPeriod(paidLate)
                // Pending/Overdue stats are not part of this specific summary method
                .totalPendingInvoices(0)
                .totalPendingBalance(BigDecimal.ZERO)
                .pendingInvoicesList(List.of())
                .totalOverdueInvoices(0)
                .totalOverdueBalance(BigDecimal.ZERO)
                .overdueInvoicesList(List.of())
                .build();
    }


    private InvoiceBasicInfo createInvoiceBasicInfo(Invoice invoice) {
        Responsible responsible = invoice.getResponsible();
        if (responsible == null || responsible.getId() == null) {
            log.warn("Invoice ID {} has no responsible or responsible ID. Cannot create InvoiceBasicInfo.", invoice.getId());
            return null;
        }

        Account arAccount;
        try {
            arAccount = accountService.findOrCreateResponsibleARAccount(responsible.getId());
        } catch (Exception e) {
            log.error("Error retrieving A/R account for responsible ID {} (Invoice ID {}): {}",
                    responsible.getId(), invoice.getId(), e.getMessage(), e);
            return null;
        }

        BigDecimal currentBalanceDue = ledgerEntryRepository.getBalanceForInvoiceOnAccount(arAccount.getId(), invoice.getId());
        log.debug("Ledger balance for Invoice ID {} on A/R Account {} is: {}", invoice.getId(), arAccount.getName(), currentBalanceDue);

        return new InvoiceBasicInfo(
                invoice.getId(),
                responsible.getId(),
                responsible.getName(),
                currentBalanceDue,
                invoice.getDueDate(),
                invoice.getStatus()
        );
    }
}
