package br.com.hyteck.school_control.services;

import br.com.hyteck.school_control.models.financial.Account;
import br.com.hyteck.school_control.models.payments.Invoice;
import br.com.hyteck.school_control.models.payments.InvoiceStatus;
import br.com.hyteck.school_control.models.payments.Responsible;
import br.com.hyteck.school_control.repositories.InvoiceRepository;
import br.com.hyteck.school_control.repositories.financial.LedgerEntryRepository;
import br.com.hyteck.school_control.services.financial.AccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Log4j2
public class InvoiceCalculationService {

    private final InvoiceRepository invoiceRepository;
    private final AccountService accountService;
    private final LedgerEntryRepository ledgerEntryRepository;

    /**
     * Calculates the total expected income for a given month.
     * This is determined by summing the current ledger-derived balances of all invoices
     * that are in PENDING or OVERDUE status for the specified reference month.
     * The ledger balance for an invoice on its A/R account reflects its original amount,
     * plus any penalties, minus any discounts, and minus any payments made.
     *
     * @param referenceMonth The month for which to calculate the total expected income.
     * @return The sum of ledger balances for all PENDING or OVERDUE invoices for the month.
     */
    public BigDecimal calcularTotalAReceberNoMes(YearMonth referenceMonth) {
        log.info("Calculating total expected income for month: {}", referenceMonth);

        List<InvoiceStatus> statuses = Arrays.asList(InvoiceStatus.PENDING, InvoiceStatus.OVERDUE);
        List<Invoice> invoices = invoiceRepository.findPendingInvoicesByMonth(referenceMonth, statuses);

        if (invoices.isEmpty()) {
            log.info("No PENDING or OVERDUE invoices found for month: {}. Total expected income is zero.", referenceMonth);
            return BigDecimal.ZERO;
        }

        log.info("Found {} PENDING or OVERDUE invoices for month: {}", invoices.size(), referenceMonth);

        BigDecimal totalExpectedIncome = BigDecimal.ZERO;

        for (Invoice invoice : invoices) {
            Responsible responsible = invoice.getResponsible();
            if (responsible == null || responsible.getId() == null) {
                log.warn("Invoice ID {} does not have a responsible party or responsible ID is null. Skipping.", invoice.getId());
                continue;
            }

            try {
                Account arAccount = accountService.findOrCreateResponsibleARAccount(responsible.getId());
                log.debug("A/R account ID {} found/created for responsible ID {}", arAccount.getId(), responsible.getId());

                BigDecimal invoiceBalance = ledgerEntryRepository.getBalanceForInvoiceOnAccount(arAccount.getId(), invoice.getId());
                log.debug("Ledger balance for Invoice ID {} on A/R Account ID {} is: {}", invoice.getId(), arAccount.getId(), invoiceBalance);

                totalExpectedIncome = totalExpectedIncome.add(invoiceBalance);
            } catch (Exception e) {
                log.error("Error processing invoice ID {} for responsible ID {}: {}", invoice.getId(), responsible.getId(), e.getMessage(), e);
                // Depending on policy, we might choose to continue or rethrow.
                // For now, logging the error and continuing to sum other invoices.
            }
        }

        log.info("Total expected income calculated for month {}: {}", referenceMonth, totalExpectedIncome);
        return totalExpectedIncome;
    }
}

