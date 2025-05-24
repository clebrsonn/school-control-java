package br.com.hyteck.school_control.models.payments;

import br.com.hyteck.school_control.models.AbstractModel;
import jakarta.persistence.*;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents a financial invoice in the system.
 * An invoice contains items, due dates, amounts, and status,
 * and is associated with a responsible party.
 */
@Entity
@Table(name = "invoices")
@Getter // Lombok: Adds getter methods for all fields.
@Setter // Lombok: Adds setter methods for all fields.
@NoArgsConstructor // Lombok: Adds a no-arguments constructor.
@AllArgsConstructor // Lombok: Adds an all-arguments constructor.
@SuperBuilder // Lombok: Enables builder pattern, including fields from superclass.
public class Invoice extends AbstractModel {

    /**
     * List of items included in this invoice.
     * Each item represents a specific charge (e.g., monthly fee, enrollment fee).
     */
    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<InvoiceItem> items = new ArrayList<>();

    /**
     * The total calculated amount of the invoice, after discounts and penalties.
     * This field is automatically calculated by the {@link #calculateTotal()} method.
     */
    /**
     * The original gross amount of the invoice, calculated as the sum of all its {@link InvoiceItem} amounts.
     * This amount represents the value before any discounts or penalties are applied via ledger entries.
     * It serves as the base value for the invoice.
     * The actual outstanding balance of the invoice is determined by querying the associated ledger entries.
     */
    @NotNull(message = "Original amount cannot be null.")
    @Positive(message = "Original amount must be positive if items exist, or zero otherwise.")
    @Column(nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal amount = BigDecimal.ZERO;

    /**
     * The date by which the invoice payment is due.
     */
    @NotNull
    @FutureOrPresent // Ensures the due date is in the present or future.
    @Column(nullable = false)
    private LocalDate dueDate;

    /**
     * The date when the invoice was issued.
     */
    @Column(nullable = false)
    private LocalDate issueDate;

    /**
     * The reference month for which this invoice is applicable (e.g., "2025-07" for July 2025).
     */
    @NotNull
    @DateTimeFormat(pattern = "yyyy-MM")
    @Column(nullable = false)
    private YearMonth referenceMonth;

    /**
     * The current status of the invoice (e.g., PENDING, PAID, OVERDUE).
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InvoiceStatus status;

    /**
     * The payment associated with this invoice, if any.
     * This is a one-to-one relationship; an invoice can have at most one payment.
     */
    @OneToOne(mappedBy = "invoice", cascade = CascadeType.ALL)
    private Payment payment;

    /**
     * An optional description or notes for the invoice.
     */
    private String description;

    /**
     * The responsible party to whom this invoice is issued.
     */
    @ManyToOne
    @JoinColumn(name = "responsible_id")
    private Responsible responsible;

    /**
     * Adds an item to the invoice and establishes a bidirectional relationship.
     *
     * @param item The {@link InvoiceItem} to add.
     */
    public void addItem(InvoiceItem item) {
        items.add(item);
        item.setInvoice(this); // Set the inverse side of the relationship.
    }

    /**
     * Callback method executed before persisting or updating the invoice.
     * It recalculates the total amount of the invoice.
     * This ensures the {@code amount} field is always up-to-date.
     */
    // @PrePersist and @PreUpdate calculateTotal() method removed.
    // The amount should be set explicitly when items are added/modified.

    /**
     * Calculates the sum of amounts from all {@link InvoiceItem}s associated with this invoice.
     * This represents the gross original amount of the invoice before any ledger-based adjustments
     * like discounts or penalties.
     *
     * @return The sum of all item amounts, or {@link BigDecimal#ZERO} if no items exist.
     */
    public BigDecimal calculateAmount() {
        // Start with the sum of all individual item amounts.
        return items.stream()
                .map(InvoiceItem::getAmount)
                .filter(Objects::nonNull) // Ensure item amounts are not null before summing
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Updates the {@code amount} field by recalculating it from the current list of items.
     * This method should be called whenever invoice items are added, removed, or their amounts change,
     * before the invoice is persisted or if the original amount needs to be re-evaluated.
     */
    public void updateAmount() {
        this.amount = calculateAmount();
    }
}
