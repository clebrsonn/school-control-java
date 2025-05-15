package br.com.hyteck.school_control.web.dtos.expenses;

import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ExpenseReport {
    private LocalDate reportDate;
    private BigDecimal totalExpenses;
    private String details;
}
