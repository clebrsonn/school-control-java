package br.com.hyteck.school_control.usecases.billing;

import java.util.List;
import java.util.stream.Collectors;

import br.com.hyteck.school_control.models.expenses.Expense;
import br.com.hyteck.school_control.models.expenses.ExpenseReport;
import br.com.hyteck.school_control.repositories.ExpenseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
public class GenerateExpenseReportUseCase {

    private final ExpenseRepository expenseRepository;

    public GenerateExpenseReportUseCase(ExpenseRepository expenseRepository) {
        this.expenseRepository = expenseRepository;
    }

    @Transactional(readOnly = true)
    public ExpenseReport execute(LocalDate startDate, LocalDate endDate) {
        List<Expense> expenses = expenseRepository.findByDateBetween(startDate, endDate);

        BigDecimal totalExpenses = expenses.stream()
                .map(Expense::getValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        String details = expenses.stream()
                .map(expense -> expense.getDescription() + ": " + expense.getValue())
                .collect(Collectors.joining("\n"));

        return ExpenseReport.builder()
                .reportDate(LocalDate.now())
                .totalExpenses(totalExpenses)
                .details(details)
                .build();
    }
}
