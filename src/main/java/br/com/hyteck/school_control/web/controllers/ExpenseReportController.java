package br.com.hyteck.school_control.web.controllers;

import br.com.hyteck.school_control.usecases.billing.GenerateExpenseReportUseCase;
import br.com.hyteck.school_control.web.dtos.expenses.ExpenseReport;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/expense-reports")
public class ExpenseReportController {

    private final GenerateExpenseReportUseCase generateExpenseReportUseCase;

    public ExpenseReportController(GenerateExpenseReportUseCase generateExpenseReportUseCase) {
        this.generateExpenseReportUseCase = generateExpenseReportUseCase;
    }

    @GetMapping
    public ResponseEntity<ExpenseReport> getExpenseReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        ExpenseReport report = generateExpenseReportUseCase.execute(startDate, endDate);
        return ResponseEntity.ok(report);
    }
}
