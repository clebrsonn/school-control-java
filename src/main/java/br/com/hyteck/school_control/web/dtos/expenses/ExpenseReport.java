package br.com.hyteck.school_control.web.dtos.expenses;

import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Data Transfer Object (DTO) representing an expense report.
 * This class encapsulates summary information about expenses, such as the report date,
 * total expenses, and any relevant details.
 * Lombok's {@link Getter}, {@link Setter}, {@link NoArgsConstructor}, {@link AllArgsConstructor},
 * and {@link SuperBuilder} annotations are used to generate boilerplate code like
 * constructors, getters, setters, and a builder.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ExpenseReport {
    /**
     * The date to which this expense report pertains or was generated.
     */
    private LocalDate reportDate;

    /**
     * The total sum of all expenses included in this report.
     */
    private BigDecimal totalExpenses;

    /**
     * A string providing additional details, notes, or a summary of the expenses in the report.
     * This can include information about the types of expenses or specific significant expenditures.
     */
    private String details;
}
