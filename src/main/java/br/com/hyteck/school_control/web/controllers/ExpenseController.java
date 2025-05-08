package br.com.hyteck.school_control.web.controllers;

import br.com.hyteck.school_control.models.expenses.Expense;
import br.com.hyteck.school_control.usecases.expenses.CreateExpenseUseCase;
import br.com.hyteck.school_control.usecases.expenses.ListExpensesUseCase;
import br.com.hyteck.school_control.usecases.expenses.UpdateExpenseUseCase;
import br.com.hyteck.school_control.web.dtos.expenses.ExpenseRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/expenses")
public class ExpenseController {

    private final CreateExpenseUseCase createExpenseUseCase;
    private final UpdateExpenseUseCase updateExpenseUseCase;
    private final ListExpensesUseCase listExpensesUseCase;

    public ExpenseController(CreateExpenseUseCase createExpenseUseCase, UpdateExpenseUseCase updateExpenseUseCase,
                             ListExpensesUseCase listExpensesUseCase) {
        this.createExpenseUseCase = createExpenseUseCase;
        this.updateExpenseUseCase = updateExpenseUseCase;
        this.listExpensesUseCase = listExpensesUseCase;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Expense> createExpense(@ModelAttribute ExpenseRequest expense) {
        Expense createdExpense = createExpenseUseCase.execute(expense);
        return ResponseEntity.ok(createdExpense);
    }

    @PutMapping(path = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Expense> updateExpense(@PathVariable String id, @ModelAttribute ExpenseRequest expense) {
        Expense createdExpense = updateExpenseUseCase.execute(id, expense);
        return ResponseEntity.ok(createdExpense);
    }


    @GetMapping
    public ResponseEntity<Page<Expense>> listAllExpenses( @PageableDefault(size = 10, sort = "date") Pageable pageable) {
        Page<Expense> expenses = listExpensesUseCase.execute(pageable);
        return ResponseEntity.ok(expenses);
    }
}