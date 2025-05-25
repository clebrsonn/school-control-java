package br.com.hyteck.school_control.models.financial;

import br.com.hyteck.school_control.models.AbstractModel;
import br.com.hyteck.school_control.models.payments.Responsible;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

/**
 * Represents a financial account in the ledger system.
 * Each account has a name, type, and a balance that reflects the sum of its ledger entries.
 * Accounts are used to categorize financial transactions (e.g., "Accounts Receivable", "Tuition Revenue").
 * This entity extends {@link AbstractModel} for common ID and audit timestamps.
 */
@Entity
@Table(name = "accounts", indexes = {
    @Index(name = "idx_account_type", columnList = "type"),
    @Index(name = "idx_account_responsible", columnList = "responsible_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Account extends AbstractModel {

    /**
     * The name of the account. This should be descriptive and unique within a certain context
     * (e.g., "Accounts Receivable - John Doe", "Tuition Revenue - 2024", "Cash").
     * Max length of 255 characters.
     */
    @NotNull(message = "Account name cannot be null.")
    @Size(min = 3, max = 255, message = "Account name must be between 3 and 255 characters.")
    @Column(nullable = false, length = 255)
    private String name;

    /**
     * The type of the account, defined by the {@link AccountType} enum.
     * This classification is crucial for financial reporting (e.g., ASSET, LIABILITY, REVENUE).
     */
    @NotNull(message = "Account type cannot be null.")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AccountType type;

    /**
     * The current balance of the account.
     * This balance is updated by posting {@link LedgerEntry} records.
     * For ASSET and EXPENSE accounts, a debit increases the balance.
     * For LIABILITY, EQUITY, and REVENUE accounts, a credit increases the balance.
     * Precision 19, scale 4 allows for large values and sufficient decimal places for financial calculations.
     */
    @NotNull(message = "Account balance cannot be null.")
    @Column(nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;

    /**
     * Optional reference to a {@link Responsible} party.
     * This is primarily used for Accounts Receivable (A/R) accounts to link the receivable
     * balance to a specific responsible person or entity.
     * For other account types (e.g., "Tuition Revenue", "Cash"), this field will typically be null.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsible_id")
    private Responsible responsible; // Nullable, for A/R accounts tied to a specific responsible

    // Convenience method to update balance - ensure this is thread-safe if needed,
    // though @Transactional in services should manage concurrency at a higher level.
    /**
     * Adds the given amount to the account's balance.
     *
     * @param amount The amount to add. Can be positive or negative.
     */
    public void addToBalance(BigDecimal amount) {
        this.balance = this.balance.add(amount);
    }

    /**
     * Subtracts the given amount from the account's balance.
     *
     * @param amount The amount to subtract. Can be positive or negative.
     */
    public void subtractFromBalance(BigDecimal amount) {
        this.balance = this.balance.subtract(amount);
    }
}
