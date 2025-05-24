package br.com.hyteck.school_control.models.expenses;

import br.com.hyteck.school_control.models.AbstractModel;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "expenses")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Expense extends AbstractModel {

    @NotNull
    @Positive
    private BigDecimal value;

    @NotNull
    private LocalDate date;

    @NotNull
    private String description;

    private String receiptUrl;
}
