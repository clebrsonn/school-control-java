package br.com.hyteck.school_control.usecases.billing;

import br.com.hyteck.school_control.models.financial.Account;
import br.com.hyteck.school_control.models.financial.LedgerEntryType;
import br.com.hyteck.school_control.models.payments.Invoice;
import br.com.hyteck.school_control.models.payments.InvoiceItem;
import br.com.hyteck.school_control.models.payments.Payment;
import br.com.hyteck.school_control.models.payments.Responsible;
import br.com.hyteck.school_control.repositories.InvoiceRepository;
import br.com.hyteck.school_control.repositories.PaymentRepository;
import br.com.hyteck.school_control.repositories.financial.LedgerEntryRepository;
import br.com.hyteck.school_control.services.financial.AccountService;
import br.com.hyteck.school_control.web.dtos.billing.InvoiceDetailDto;
import br.com.hyteck.school_control.web.dtos.billing.InvoiceItemDetailDto;
import br.com.hyteck.school_control.web.dtos.payments.PaymentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Log4j2
public class GetInvoiceDetailsUseCase {

    private final InvoiceRepository invoiceRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final AccountService accountService;
    private final PaymentRepository paymentRepository;

    @Transactional(readOnly = true)
    public Optional<InvoiceDetailDto> execute(String invoiceId) {
        log.info("Fetching details for invoice ID: {}", invoiceId);

        Optional<Invoice> invoiceOpt = invoiceRepository.findById(invoiceId);
        if (invoiceOpt.isEmpty()) {
            log.warn("Invoice ID {} not found.", invoiceId);
            return Optional.empty();
        }
        Invoice invoice = invoiceOpt.get();

        Responsible responsible = invoice.getResponsible();
        if (responsible == null || responsible.getId() == null) {
            log.error("Invoice ID {} has no responsible or responsible ID. Cannot proceed.", invoiceId);
            // This should ideally not happen for a valid invoice
            return Optional.empty();
        }

        Account arAccount;
        try {
            arAccount = accountService.findOrCreateResponsibleARAccount(responsible.getId());
            log.debug("A/R Account ID {} found/created for responsible ID {}", arAccount.getId(), responsible.getId());
        } catch (Exception e) {
            log.error("Error retrieving A/R account for responsible ID {}: {}. Cannot fetch invoice details.",
                    responsible.getId(), e.getMessage(), e);
            return Optional.empty();
        }

        // Fetch specific ledger summaries
        // totalAdHocDiscountsApplied refers to discounts applied directly via ledger, NOT itemized ones.
        BigDecimal totalAdHocDiscountsApplied = ledgerEntryRepository.sumCreditAmountByInvoiceIdAndAccountIdAndType(
                invoiceId, arAccount.getId(), LedgerEntryType.DISCOUNT_APPLIED
        );
        log.debug("Total Ad-Hoc Discounts Applied (Ledger) for Invoice ID {}: {}", invoiceId, totalAdHocDiscountsApplied);

        BigDecimal totalPenaltiesAssessed = ledgerEntryRepository.sumDebitAmountByInvoiceIdAndAccountIdAndType(
                invoiceId, arAccount.getId(), LedgerEntryType.PENALTY_ASSESSED
        );
        log.debug("Total Penalties Assessed (Ledger) for Invoice ID {}: {}", invoiceId, totalPenaltiesAssessed);
        
        BigDecimal totalPaymentsReceived = ledgerEntryRepository.sumCreditAmountByInvoiceIdAndAccountIdAndType(
                invoiceId, arAccount.getId(), LedgerEntryType.PAYMENT_RECEIVED
        );
        log.debug("Total Payments Received (Ledger) for Invoice ID {}: {}", invoiceId, totalPaymentsReceived);

        // currentBalanceDue is the definitive balance from the A/R ledger for this invoice.
        BigDecimal currentBalanceDue = ledgerEntryRepository.getBalanceForInvoiceOnAccount(arAccount.getId(), invoiceId);
        log.debug("Ledger-derived current balance for Invoice ID {} on A/R Account {} is: {}",
                invoiceId, arAccount.getName(), currentBalanceDue);


        List<InvoiceItemDetailDto> itemDtos = invoice.getItems().stream()
                .map(this::mapToInvoiceItemDetailDto)
                .collect(Collectors.toList());

        List<Payment> payments = paymentRepository.findByInvoiceId(invoiceId);
        List<PaymentResponse> paymentDtos = payments.stream()
                .map(PaymentResponse::from)
                .collect(Collectors.toList());
        
        // Verify calculated balance if needed (originalAmount - discounts + penalties - payments)
        // BigDecimal calculatedBalance = invoice.getOriginalAmount()
        // .subtract(totalAdHocDiscountsApplied) // This would be part of a manual recalculation of currentBalanceDue
        // .add(totalPenaltiesAssessed)
        // .subtract(totalPaymentsReceived);
        // log.debug("Manually calculated balance (Net Original - AdHoc Discounts + Penalties - Payments): {}", calculatedBalance);
        // if (currentBalanceDue.compareTo(calculatedBalance) != 0) {
        //     log.warn("Ledger balance {} and calculated balance {} mismatch for invoice {}", currentBalanceDue, calculatedBalance, invoiceId);
        // }

        return Optional.of(InvoiceDetailDto.builder()
                .id(invoice.getId())
                .responsibleId(responsible.getId())
                .responsibleName(responsible.getName())
                .referenceMonth(invoice.getReferenceMonth())
                .issueDate(invoice.getIssueDate())
                .dueDate(invoice.getDueDate())
                .status(invoice.getStatus())
                .originalAmount(invoice.getAmount()) // This is the NET amount from Invoice items
                .totalAdHocDiscountsApplied(totalAdHocDiscountsApplied)
                .totalPenaltiesAssessed(totalPenaltiesAssessed)
                .totalPaymentsReceived(totalPaymentsReceived)
                .currentBalanceDue(currentBalanceDue)
                .items(itemDtos)
                .payments(paymentDtos)
                .build());
    }

    private InvoiceItemDetailDto mapToInvoiceItemDetailDto(InvoiceItem item) {
        String enrollmentInfo = "N/A";
        if (item.getEnrollment() != null) {
            enrollmentInfo = item.getEnrollment().getStudent().getName() + " - " +
                             item.getEnrollment().getClassroom().getName();
        }
        return new InvoiceItemDetailDto(
                item.getId(),
                item.getDescription(),
                item.getAmount(),
                enrollmentInfo,
                item.getType() != null ? item.getType().name() : "N/A"
        );
    }
}
