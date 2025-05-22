package br.com.hyteck.school_control.usecases.billing;

import br.com.hyteck.school_control.models.classrooms.Enrollment;
import br.com.hyteck.school_control.models.payments.*;
import br.com.hyteck.school_control.models.financial.Account;
import br.com.hyteck.school_control.models.financial.AccountType;
import br.com.hyteck.school_control.models.financial.LedgerEntryType;
import br.com.hyteck.school_control.repositories.DiscountRepository;
import br.com.hyteck.school_control.repositories.EnrollmentRepository;
import br.com.hyteck.school_control.repositories.InvoiceRepository;
import br.com.hyteck.school_control.services.financial.AccountService;
import br.com.hyteck.school_control.events.BatchInvoiceGeneratedEvent;
import br.com.hyteck.school_control.services.financial.LedgerService;
// import br.com.hyteck.school_control.usecases.notification.CreateNotification; // To be removed
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher; // Added
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
public class GenerateInvoicesForParents {
    private final DiscountRepository discountRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final InvoiceRepository invoiceRepository;
    private final CreateNotification createNotification;

    // Constants for Brazilian Portuguese locale, date and currency formatting.
    private static final Locale BRAZIL_LOCALE = Locale.of("pt", "BR");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy", BRAZIL_LOCALE);
    private static final NumberFormat CURRENCY_FORMATTER = NumberFormat.getCurrencyInstance(BRAZIL_LOCALE);

    /**
     * Constructs a new GenerateInvoicesForParents use case.
     *
     * @param enrollmentRepository Repository for accessing enrollment data.
     * @param invoiceRepository    Repository for saving invoice data.
     * @param createNotification   Use case for creating notifications.
     * @param discountRepository   Repository for accessing discount data.
     */
    public GenerateInvoicesForParents(EnrollmentRepository enrollmentRepository,
                                      InvoiceRepository invoiceRepository,
                                      CreateNotification createNotification,
                                      DiscountRepository discountRepository) {
        this.enrollmentRepository = enrollmentRepository;
        this.invoiceRepository = invoiceRepository;
        this.createNotification = createNotification;
        this.discountRepository = discountRepository;
    }

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

            // Apply discount if this is the second (or more) student for the same responsible on this invoice
            // and a discount is available.
            if (monthlyInvoice.getItems().size() > 0) { // Indicates more than one student for this responsible
                monthlyFeeDiscount.ifPresent(discount -> {
                    // Ensure discount is not already added
                    if (monthlyInvoice.getDiscounts().stream().noneMatch(d -> d.getId().equals(discount.getId()))) {
                        monthlyInvoice.getDiscounts().add(discount);
                        log.info("Applied discount '{}' to invoice for responsible {}", discount.getDescription(), responsible.getId());
                    }
                });
            }


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
            // monthlyInvoice.setAmount(monthlyInvoice.calculateTotalAmount());
        }

        // Save all created or updated invoices.
        if (!invoicesByResponsibleId.isEmpty()) {
            List<Invoice> savedInvoices = invoiceRepository.saveAll(invoicesByResponsibleId.values());
            log.info("{} monthly invoices saved successfully for month {}.", savedInvoices.size(), targetMonth);

            // Send notifications to responsibles about the new invoices.
            savedInvoices.forEach(invoice -> {
                Responsible responsible = invoice.getResponsible();
                // Ensure the responsible has an associated user account for notifications.
                if (responsible.getUser() == null || responsible.getUser().getId() == null) {
                    log.warn("Responsible ID {} (Name: {}) has no associated user account. Cannot send notification for invoice ID {}.",
                            responsible.getId(), responsible.getName(), invoice.getId());
                    return;
                }
                String userIdForNotification = responsible.getUser().getId();

                // It's better to list student names if multiple, or use a generic message.
                String studentNames = invoice.getItems().stream()
                        .map(item -> item.getEnrollment().getStudent().getName())
                        .distinct()
                        .reduce((s1, s2) -> s1 + ", " + s2)
                        .orElse("N/D");

                String formattedAmount = CURRENCY_FORMATTER.format(invoice.getAmount()); // Amount is calculated by PrePersist/PreUpdate
                String formattedDueDate = invoice.getDueDate().format(DATE_FORMATTER);

                String notificationMessage = String.format(
                        "Nova fatura de mensalidade (Ref: %s) gerada para %s no valor de %s, com vencimento em %s.",
                        targetMonth.format(DateTimeFormatter.ofPattern("MM/yyyy")),
                        studentNames,
                        formattedAmount,
                        formattedDueDate
                );
                String notificationLink = "/invoices/" + invoice.getId(); // Frontend link to view the invoice.
                String notificationType = "NEW_MONTHLY_INVOICE";

                try {
                    createNotification.execute(
                            userIdForNotification,
                            notificationMessage,
                            notificationLink,
                            notificationType
                    );
                    log.info("Notification for new invoice ID {} sent to user ID {}.", invoice.getId(), userIdForNotification);
                } catch (Exception e) {
                    log.error("Failed to send notification for invoice ID {} to user ID {}: {}", invoice.getId(), userIdForNotification, e.getMessage(), e);
                    // Decide if failure to notify should impact the transaction (typically not).
                }
            });
        } else {
            log.info("No new monthly invoices generated for month {}.", targetMonth);
        }
        log.info("Monthly invoice generation for {} completed.", targetMonth);
    }
}