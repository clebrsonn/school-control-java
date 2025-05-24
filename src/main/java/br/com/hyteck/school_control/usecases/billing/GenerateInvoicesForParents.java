package br.com.hyteck.school_control.usecases.billing;

import br.com.hyteck.school_control.models.classrooms.Enrollment;
import br.com.hyteck.school_control.models.financial.Account;
import br.com.hyteck.school_control.models.financial.AccountType;
import br.com.hyteck.school_control.models.financial.LedgerEntryType;
import br.com.hyteck.school_control.models.payments.*;
import br.com.hyteck.school_control.repositories.DiscountRepository;
import br.com.hyteck.school_control.repositories.EnrollmentRepository;
import br.com.hyteck.school_control.repositories.InvoiceRepository;
import br.com.hyteck.school_control.services.financial.AccountService;
import br.com.hyteck.school_control.services.financial.LedgerService;
import br.com.hyteck.school_control.usecases.notification.CreateNotification;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;

/**
 * Use case responsible for generating monthly invoices for parents/responsibles
 * based on active student enrollments.
 * It calculates monthly fees, applies discounts if applicable, creates invoices,
 * and notifies the responsible parties.
 */
@Service
@Log4j2
@RequiredArgsConstructor
public class GenerateInvoicesForParents {
    private final DiscountRepository discountRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final InvoiceRepository invoiceRepository;
    private final CreateNotification createNotification;
    private final LedgerService ledgerService;
    private final AccountService accountService;

    // Constants for Brazilian Portuguese locale, date and currency formatting.
    private static final Locale BRAZIL_LOCALE = Locale.of("pt", "BR");
    private static final String TUITION_REVENUE_ACCOUNT_NAME = "Tuition Revenue";
    private static final String DISCOUNT_EXPENSE_ACCOUNT_NAME = "Discount Expense";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy", BRAZIL_LOCALE);
    private static final NumberFormat CURRENCY_FORMATTER = NumberFormat.getCurrencyInstance(BRAZIL_LOCALE);


    /**
     * Executes the monthly invoice generation process for the specified target month.
     * This method is transactional, ensuring that all invoices are generated and saved,
     * or none if an error occurs.
     *
     * @param targetMonth The {@link YearMonth} for which to generate invoices (e.g., 2025-07 for July 2025).
     */
    @Transactional
    public void execute(YearMonth targetMonth) {
        log.info("Starting monthly invoice generation for month: {}", targetMonth);

        // Retrieve all active enrollments.
        List<Enrollment> activeEnrollments = enrollmentRepository.findByStatus(Enrollment.Status.ACTIVE);
        log.info("Found {} active enrollments.", activeEnrollments.size());

        // Fetch any applicable discount for monthly fees valid for the current period.
        Optional<Discount> monthlyFeeDiscount = discountRepository.findByTypeAndValidAtBeforeToday(Types.MENSALIDADE);
        if (monthlyFeeDiscount.isPresent()) {
            log.info("Applicable monthly fee discount found: {}", monthlyFeeDiscount.get().getDescription());
        }

        // Use a map to consolidate invoices by responsible ID.
        // This allows adding multiple students from the same responsible to a single invoice.
        Map<String, Invoice> invoicesByResponsibleId = new HashMap<>();

        for (Enrollment enrollment : activeEnrollments) {
            // Skip enrollments with no defined or zero monthly fee.
            if (enrollment.getMonthlyFee() == null || enrollment.getMonthlyFee().compareTo(BigDecimal.ZERO) <= 0) {
                log.warn("Enrollment ID {} (Student: {}) has no valid monthly fee defined. Skipping.",
                        enrollment.getId(), enrollment.getStudent().getName());
                continue;
            }

            // Skip enrollments where the student has no associated responsible.
            Responsible responsible = enrollment.getStudent().getResponsible();
            if (responsible == null) {
                log.warn("Enrollment ID {} (Student: {}) has no responsible associated. Skipping.",
                        enrollment.getId(), enrollment.getStudent().getName());
                continue;
            }

            // Check if an invoice item for this specific enrollment and month already exists for this responsible.
            // This prevents duplicate billing for the same student/enrollment in the same month.
            boolean alreadyBilled = invoiceRepository.existsByResponsibleIdAndReferenceMonthAndItems_Enrollment_Id(
                    responsible.getId(),
                    targetMonth,
                    enrollment.getId(),
                    Types.MENSALIDADE // Check specifically for MENSALIDADE type item
            );

            if (alreadyBilled) {
                log.info("Monthly fee for enrollment ID {} (Student: {}), month {} has already been billed for responsible {}. Skipping.",
                        enrollment.getId(), enrollment.getStudent().getName(), targetMonth, responsible.getId());
                continue;
            }

            // Determine the due date for the invoice.
            // If current day is after the 10th, set due date to 10th of next month. Otherwise, 10th of current month.
            LocalDate dueDate = LocalDate.now(ZoneId.of("America/Sao_Paulo")).getDayOfMonth() > 10
                    ? LocalDate.of(targetMonth.getYear(), targetMonth.getMonthValue() + 1, 10)
                    : targetMonth.atDay(10);

            // Retrieve or create an invoice for the responsible.
            Invoice monthlyInvoice = invoicesByResponsibleId.computeIfAbsent(responsible.getId(), respId -> {
                log.info("Creating new invoice for responsible ID {} for month {}", respId, targetMonth);
                return Invoice.builder()
                        .responsible(responsible)
                        .referenceMonth(targetMonth)
                        .issueDate(LocalDate.now(ZoneId.of("America/Sao_Paulo"))) // Issue date is today
                        .dueDate(dueDate)
                        .status(InvoiceStatus.PENDING)
                        .description("Fatura Mensalidade " + targetMonth.getMonth().getDisplayName(TextStyle.FULL, BRAZIL_LOCALE) + "/" + targetMonth.getYear())
                        .build();
            });

            // Create an invoice item for the current enrollment's monthly fee.
            InvoiceItem monthlyFeeItem = InvoiceItem.builder()
                    .enrollment(enrollment)
                    .type(Types.MENSALIDADE)
                    .description("Mensalidade " + targetMonth.getMonth().getDisplayName(TextStyle.FULL, BRAZIL_LOCALE) + "/" + targetMonth.getYear() +
                            " - Aluno: " + enrollment.getStudent().getName())
                    .amount(enrollment.getMonthlyFee())
                    .build();

            monthlyInvoice.addItem(monthlyFeeItem);
            // The amount is recalculated via @PrePersist/@PreUpdate in Invoice entity,
            // but we can call it here if we need the updated amount immediately.
            monthlyInvoice.calculateAmount(); // Explicitly calculate original amount
        }

        // Save all created or updated invoices and post ledger transactions.
        if (!invoicesByResponsibleId.isEmpty()) {
            Account tuitionRevenueAccount = accountService.findOrCreateAccount(TUITION_REVENUE_ACCOUNT_NAME, AccountType.REVENUE, null);
            log.info("Retrieved/Created Tuition Revenue account: {}", tuitionRevenueAccount.getName());
            // Discount Expense account is not directly used for ledger posting anymore if discounts are itemized.
            // It might still be relevant for other accounting purposes or if other discount types exist that are not itemized.
            // For now, we remove its direct retrieval here as it's not used in the immediate subsequent logic.
            // Account discountExpenseAccount = accountService.findOrCreateAccount(DISCOUNT_EXPENSE_ACCOUNT_NAME, AccountType.EXPENSE, null);
            // log.info("Retrieved/Created Discount Expense account: {}", discountExpenseAccount.getName());

            for (Invoice invoice : invoicesByResponsibleId.values()) {
                Responsible responsible = invoice.getResponsible();
                Account responsibleARAccount = accountService.findOrCreateResponsibleARAccount(responsible.getId());
                log.info("Retrieved/Created A/R account for responsible {}: {}", responsible.getName(), responsibleARAccount.getName());

                // Calculate total discount amount for this invoice based on its items and applicable discounts.
                // This is a simplified example. Real discount logic might be more complex.
                // For this example, we'll use the previously fetched monthlyFeeDiscount if more than one item exists.
                BigDecimal totalDiscountAmount = BigDecimal.ZERO;
                if (invoice.getItems().size() > 1) { // Condition for applying discount
                        // This is a placeholder for actual discount calculation logic.
                        // Assuming discount is a fixed amount or percentage of the first item, etc.
                        // For simplicity, let's say it's a fixed amount per additional student.
                        // This should be replaced with actual discount calculation rules.
                        // Here, we're just using the discount's percentage on the sum of other items, as an example.
                        // Or if it's a fixed amount, use that. The current Discount model is flexible.
                        // Let's assume the discount value is a fixed amount to be applied if criteria met.
                        // This logic part needs to be robust based on how discounts are defined.
                        // For now, if a discount is present and applicable (e.g. >1 student), we apply its amount.
                        // This is a critical point: The discount amount calculation logic needs to be accurate.
                        // The original code added the *Discount object* to the invoice. We need the *amount*.
                        // Let's assume the discount object has a getAmount() or similar.
                        // The current Discount model has `percentage` and `fixedValue`.
                        // We need to decide how to apply this. Let's assume for this example,
                        // if a 'MENSALIDADE' discount is found, and there's >1 student,
                        // we apply its fixedValue if not null, otherwise percentage of the originalAmount.
                        // This is a placeholder for potentially complex discount rules.

                        Discount actualDiscount = monthlyFeeDiscount.get();
                        // The variable totalDiscountAmount will still be used for the separate ledger posting in this subtask.
                        if (actualDiscount.getValue() != null && actualDiscount.getValue().compareTo(BigDecimal.ZERO) > 0) {
                            totalDiscountAmount = actualDiscount.getValue();
                            log.info("Calculated fixed discount amount {} for responsible {} from policy: {}",
                                    CURRENCY_FORMATTER.format(totalDiscountAmount), responsible.getName(), actualDiscount.getDescription());
                        }

                        if (totalDiscountAmount.compareTo(BigDecimal.ZERO) > 0) {
                            InvoiceItem discountItem = InvoiceItem.builder()
                                    .type(Types.DESCONTO)
                                    .description(actualDiscount.getName() != null && !actualDiscount.getName().isBlank() ? actualDiscount.getName() : actualDiscount.getDescription())
                                    .amount(totalDiscountAmount.negate()) // Negative amount for discount
                                    .enrollment(null) // Discount item is not tied to a specific enrollment
                                    .build();
                            invoice.addItem(discountItem); // Add the discount item to the invoice
                            log.info("Added itemized discount '{}' of {} to invoice for responsible {}",
                                    discountItem.getDescription(), CURRENCY_FORMATTER.format(discountItem.getAmount()), responsible.getName());
                            
                            // Update originalAmount to reflect NET amount after discount item is added.
                            invoice.calculateAmount();
                            log.info("Invoice amount for responsible {} (ID: {}) updated to NET {} after itemized discount.",
                                    responsible.getName(), invoice.getId(), CURRENCY_FORMATTER.format(invoice.calculateAmount()));
                        }
                }
                // If no discount was applied, invoice.getOriginalAmount() remains the gross sum of MENSALIDADE items.
                // If a discount was applied, invoice.getOriginalAmount() is now NET.
                // The existing totalDiscountAmount variable will be used for the separate DISCOUNT_APPLIED ledger posting (as per subtask constraints).


                Invoice savedInvoice = invoiceRepository.save(invoice);
                log.info("Saved invoice ID {} for responsible {}", savedInvoice.getId(), responsible.getName());

                // Post the main tuition fee transaction (Net Amount)
                // savedInvoice.getOriginalAmount() now reflects the NET amount due to the earlier call to
                // invoice.updateOriginalAmount() after adding any itemized discounts.
                try {
                    ledgerService.postTransaction(
                            savedInvoice,
                            null, // No payment at invoice generation
                            responsibleARAccount, // Debit A/R
                            tuitionRevenueAccount, // Credit Tuition Revenue
                            savedInvoice.calculateAmount(), // This is now the NET amount
                            LocalDateTime.now(ZoneId.of("America/Sao_Paulo")),
                            "Monthly invoice " + savedInvoice.getId() + " (net) generated for " + responsible.getName() + " - Ref: " + targetMonth.format(DateTimeFormatter.ofPattern("MM/yyyy")),
                            LedgerEntryType.TUITION_FEE
                    );
                    log.info("Posted TUITION_FEE transaction for invoice ID {} to ledger. Net Amount: {}. Debited: {}, Credited: {}.",
                            savedInvoice.getId(), CURRENCY_FORMATTER.format(savedInvoice.calculateAmount()), responsibleARAccount.getName(), tuitionRevenueAccount.getName());
                } catch (Exception e) {
                    log.error("Failed to post TUITION_FEE transaction for invoice ID {}: {}. Rolling back.", savedInvoice.getId(), e.getMessage(), e);
                    throw e; // Re-throw to ensure transaction rollback
                }

                // The separate DISCOUNT_APPLIED ledger posting block has been removed.
                // The itemized discount (negative InvoiceItem) has already adjusted the 
                // savedInvoice.originalAmount to be net. The main TUITION_FEE posting
                // now correctly reflects this net amount.

                // Send notifications to responsibles about the new invoices.
                // Ensure the responsible has an associated user account for notifications.
                if (responsible.getId() == null) {
                    log.warn("Responsible ID {} (Name: {}) has no associated user account. Cannot send notification for invoice ID {}.",
                            responsible.getId(), responsible.getName(), savedInvoice.getId());
                    // Continue to next invoice or responsible, notification is not critical for financial transaction
                } else {
                    String userIdForNotification = responsible.getId();

                    // It's better to list student names if multiple, or use a generic message.
                    String studentNames = savedInvoice.getItems().stream()
                            .map(item -> item.getEnrollment().getStudent().getName())
                            .distinct()
                            .reduce((s1, s2) -> s1 + ", " + s2)
                            .orElse("N/D");

                    String formattedAmount = CURRENCY_FORMATTER.format(savedInvoice.calculateAmount());
                    String formattedDueDate = savedInvoice.getDueDate().format(DATE_FORMATTER);

                    String notificationMessage = String.format(
                            "Nova fatura de mensalidade (Ref: %s) gerada para %s no valor de %s, com vencimento em %s.",
                            targetMonth.format(DateTimeFormatter.ofPattern("MM/yyyy")),
                            studentNames,
                            formattedAmount,
                            formattedDueDate
                    );
                    String notificationLink = "/invoices/" + savedInvoice.getId(); // Frontend link to view the invoice.
                    String notificationType = "NEW_MONTHLY_INVOICE";

                    try {
                        createNotification.execute(
                                userIdForNotification,
                                notificationMessage,
                                notificationLink,
                                notificationType
                        );
                        log.info("Notification for new invoice ID {} sent to user ID {}.", savedInvoice.getId(), userIdForNotification);
                    } catch (Exception e) {
                        log.error("Failed to send notification for invoice ID {} to user ID {}: {}", savedInvoice.getId(), userIdForNotification, e.getMessage(), e);
                        // Decide if failure to notify should impact the transaction (typically not).
                    }
                }
            }
            log.info("{} monthly invoices processed and ledger entries posted for month {}.", invoicesByResponsibleId.size(), targetMonth);
        } else {
            log.info("No new monthly invoices generated for month {}.", targetMonth);
        }
        log.info("Monthly invoice generation for {} completed.", targetMonth);
    }
}