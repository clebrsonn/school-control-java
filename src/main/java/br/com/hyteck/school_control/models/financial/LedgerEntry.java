package br.com.hyteck.school_control.models.financial;

import br.com.hyteck.school_control.models.AbstractModel;
import br.com.hyteck.school_control.models.payments.Invoice;
import br.com.hyteck.school_control.models.payments.Payment;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Check;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents a single entry in the financial ledger.
 * Each ledger entry records a debit or a credit to a specific account,
 * associated with a transaction date, description, and type.
 * This entity extends {@link AbstractModel} for common ID and audit timestamps.
 *
 * A fundamental accounting principle is that for any transaction, total debits must equal total credits.
 * This entity represents one side of that transaction (either the debit or the credit).
 * A complete transaction typically involves at least two LedgerEntry records.
 */
@Entity
@Table(name = "ledger_entries", indexes = {
    @Index(name = "idx_ledger_account_id", columnList = "account_id"),
    @Index(name = "idx_ledger_invoice_id", columnList = "invoice_id"),
    @Index(name = "idx_ledger_payment_id", columnList = "payment_id"),
    @Index(name = "idx_ledger_transaction_date", columnList = "transactionDate")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@ToString(callSuper = true)
// Hibernate-specific check constraint. For portability, a JPA Bean Validation custom constraint or service-layer validation is better.
// This constraint ensures that either debitAmount or creditAmount is greater than zero, but not both simultaneously being greater than zero,
// and also not both being zero (or less, though @DecimalMin handles positive).
// A stricter version would be (debitAmount > 0 AND creditAmount = 0) OR (creditAmount > 0 AND debitAmount = 0)
@Check(constraints = "(debit_amount > 0 AND credit_amount = 0) OR (credit_amount > 0 AND debit_amount = 0)")
public class LedgerEntry extends AbstractModel {

    /**
     * The account to which this ledger entry pertains.
     * Each entry must be associated with exactly one account.
     */
    @NotNull(message = "Account cannot be null for a ledger entry.")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    /**
     * Optional reference to the {@link Invoice} related to this ledger entry.
     * For example, entries for tuition fees, discounts, or invoice-related penalties.
     * Can be null for general journal entries or payments not directly tied to a specific invoice (e.g., bulk payments).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id")
    private Invoice invoice;

    /**
     * Optional reference to the {@link Payment} related to this ledger entry.
     * For example, entries recording the receipt of a payment.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id")
    private Payment payment;

    /**
     * The debit amount of the entry.
     * Represents an increase for ASSET and EXPENSE accounts, and a decrease for LIABILITY, EQUITY, and REVENUE accounts.
     * Defaults to zero. Must be non-negative.
     * If debitAmount > 0, then creditAmount must be 0.
     */
    @NotNull(message = "Debit amount cannot be null.")
    @DecimalMin(value = "0.0", message = "Debit amount must be non-negative.")
    @Column(name = "debit_amount", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal debitAmount = BigDecimal.ZERO;

    /**
     * The credit amount of the entry.
     * Represents a decrease for ASSET and EXPENSE accounts, and an increase for LIABILITY, EQUITY, and REVENUE accounts.
     * Defaults to zero. Must be non-negative.
     * If creditAmount > 0, then debitAmount must be 0.
     */
    @NotNull(message = "Credit amount cannot be null.")
    @DecimalMin(value = "0.0", message = "Credit amount must be non-negative.")
    @Column(name = "credit_amount", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal creditAmount = BigDecimal.ZERO;

    /**
     * The date and time when the transaction occurred or was recorded.
     * This is crucial for chronological ordering of entries and financial reporting.
     */
    @NotNull(message = "Transaction date cannot be null.")
    @Column(nullable = false)
    private LocalDateTime transactionDate;

    /**
     * A description of the ledger entry, providing context for the transaction.
     * (e.g., "Monthly tuition fee for John Doe - July 2024", "Payment received via bank transfer").
     * Max length of 500 characters.
     */
    @NotNull(message = "Description cannot be null.")
    @Size(min = 3, max = 500, message = "Description must be between 3 and 500 characters.")
    @Column(nullable = false, length = 500)
    private String description;

    /**
     * The type of transaction or event this ledger entry represents, defined by {@link LedgerEntryType}.
     * This helps in categorizing and understanding the nature of the financial posting.
     */
    @NotNull(message = "Ledger entry type cannot be null.")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private LedgerEntryType type;

    /**
     * Pre-persistence and pre-update validation to ensure data integrity.
     * Specifically, checks that not both debit and credit amounts are greater than zero,
     * and that not both are zero (one must be greater than zero).
     *
     * Note: The @Check constraint at the class level provides DB-level validation.
     * This method provides application-level validation before persisting.
     */
    @PrePersist
    @PreUpdate
    protected void validateAmounts() {
        boolean debitIsPositive = debitAmount != null && debitAmount.compareTo(BigDecimal.ZERO) > 0;
        boolean creditIsPositive = creditAmount != null && creditAmount.compareTo(BigDecimal.ZERO) > 0;

        if (debitIsPositive && creditIsPositive) {
            throw new IllegalStateException("Ledger entry cannot have both debit and credit amounts greater than zero.");
        }
        if (!debitIsPositive && !creditIsPositive) {
            throw new IllegalStateException("Ledger entry must have either a debit or a credit amount greater than zero.");
        }
        // Also ensure that if one is positive, the other is zero (covered by @Check, but good for defense)
        // Stricter validation: if one amount is positive, the other *must* be zero.
        // This complements the @Check constraint at the DB level and ensures integrity at application level.
        // @Builder.Default already initializes amounts to BigDecimal.ZERO.
        if (debitIsPositive && (creditAmount == null || creditAmount.compareTo(BigDecimal.ZERO) != 0)) {
             throw new IllegalStateException("If debitAmount is positive, creditAmount must be zero. Found creditAmount: " + creditAmount);
        }
        if (creditIsPositive && (debitAmount == null || debitAmount.compareTo(BigDecimal.ZERO) != 0)) {
            throw new IllegalStateException("If creditAmount is positive, debitAmount must be zero. Found debitAmount: " + debitAmount);
        }
        // Ensure that if one field is null (despite @NotNull, for programmatic construction safety before validation)
        // and the other is positive, the null one is treated as zero for the above checks.
        // However, @NotNull on the fields themselves should prevent nulls from reaching here via JPA/validation.
        // The @Builder.Default to BigDecimal.ZERO is the primary mechanism ensuring they are not null.
    }
}
