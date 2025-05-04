package br.com.hyteck.school_control.models.expenses;

import br.com.hyteck.school_control.models.AbstractModel;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Getter
@Setter
@Builder
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
