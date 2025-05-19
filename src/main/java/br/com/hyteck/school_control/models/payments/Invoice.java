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
import java.util.Optional;

@Entity
@Table(name = "invoices")
@Getter // Lombok
@Setter // Lombok
@NoArgsConstructor // Lombok
@AllArgsConstructor // Lombok
@SuperBuilder
public class Invoice extends AbstractModel {

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<InvoiceItem> items = new ArrayList<>();

    @NotNull
    @Positive
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @NotNull
    @FutureOrPresent
    @Column(nullable = false)
    private LocalDate dueDate;

    @Column(nullable = false)
    private LocalDate issueDate;

    @NotNull
    @DateTimeFormat(pattern = "yyyy-MM")
    @Column(nullable = false)
    private YearMonth referenceMonth;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InvoiceStatus status;

    @OneToOne(mappedBy = "invoice", cascade = CascadeType.ALL)
    private Payment payment;

    private String description;

    @ManyToMany
    @JoinTable(
            name = "invoice_discounts",
            joinColumns = @JoinColumn(name = "invoice_id"),
            inverseJoinColumns = @JoinColumn(name = "discount_id")
    )
    @Builder.Default
    private List<Discount> discounts = new ArrayList<>();

    @Column(name = "penalty", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal penalty = BigDecimal.ZERO; // valor fixo de R$10,00

    @ManyToOne
    @JoinColumn(name = "responsible_id")
    private Responsible responsible;

    public void addItem(InvoiceItem item) {
        items.add(item);
        item.setInvoice(this);
    }

    @PrePersist
    @PreUpdate
    private void calculateTotal() {
        this.amount = calculateTotalAmount();
    }

    public BigDecimal calculateTotalAmount() {
        BigDecimal discountCalculated = BigDecimal.ZERO;
        BigDecimal total = items.stream()
                .map(InvoiceItem::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        for (Discount discount : discounts) {
            Optional<InvoiceItem> item = items.stream().filter(invoiceItem -> invoiceItem.getType().equals(discount.getType())).findFirst();
            if (item.isPresent()) {
                discountCalculated = discountCalculated.add(discount.getValue());
            }
        }
        total = total.subtract(discountCalculated.max(BigDecimal.ZERO));

        if (payment != null && payment.getPaymentDate().isAfter(dueDate.atStartOfDay())) {
            penalty= BigDecimal.TEN;
            total = total.add(penalty);
        }

        return total.max(BigDecimal.ZERO);
    }
}
