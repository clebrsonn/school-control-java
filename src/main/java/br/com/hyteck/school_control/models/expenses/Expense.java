package br.com.hyteck.school_control.models.expenses;

import br.com.hyteck.school_control.models.AbstractModel;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Getter
@Setter
public class Expense extends AbstractModel {

    @NotNull
    private BigDecimal value;

    @NotNull
    private LocalDate date;

    @NotNull
    private String description;

    private String receiptUrl;
}
