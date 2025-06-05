// E:/IdeaProjects/school-control-java/src/main/java/br/com/hyteck/school_control/models/invoice/InvoiceItem.java
package br.com.hyteck.school_control.models.payments;

import br.com.hyteck.school_control.models.AbstractModel;
import br.com.hyteck.school_control.models.classrooms.Enrollment;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "invoice_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceItem extends AbstractModel {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enrollment_id")
    private Enrollment enrollment;

    @Enumerated(value = EnumType.STRING)
    private Types type;

    // Opcional: Outros campos para rastrear a origem (ex: qual serviço, qual produto)
    // private String sourceType; // Ex: "ENROLLMENT", "FEE", "PRODUCT"
    // private String sourceId;   // Ex: ID do Enrollment, ID da Fee, ID do Product

    // Métodos utilitários, se necessário
    // public void updateAmount(BigDecimal newAmount) { // Removed
    //     if (newAmount == null || newAmount.compareTo(BigDecimal.ZERO) <= 0) {
    //         throw new IllegalArgumentException("New amount cannot be null or non-positive");
    //     }
    //     this.amount = newAmount;
    // }
}