package br.com.hyteck.school_control.models.financials;

import br.com.hyteck.school_control.models.AbstractModel;
import br.com.hyteck.school_control.models.payments.Responsible;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;


@Entity
@Table(name = "accounts", indexes = {
        @Index(columnList = "type"),
        @Index(columnList = "responsible_id")
})
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@SuperBuilder
public class Account extends AbstractModel {

    @NotNull
    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @NotNull
    @Column(nullable = false)
    private AccountType type;

    @Builder.Default
    private BigDecimal balance= BigDecimal.ZERO;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsible_id")
    private Responsible responsible;
}
