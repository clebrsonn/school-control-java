package br.com.hyteck.school_control.usecases.expenses;

import br.com.hyteck.school_control.models.expenses.Expense;
import br.com.hyteck.school_control.repositories.ExpenseRepository;
import br.com.hyteck.school_control.web.dtos.expenses.ExpenseRequest;
import org.springframework.stereotype.Service;

@Service
public class CreateExpenseUseCase {
    private final ExpenseRepository expenseRepository;
    private final StorageService storageService;
    public CreateExpenseUseCase(ExpenseRepository expenseRepository, StorageService storageService) {
        this.expenseRepository = expenseRepository;
        this.storageService = storageService;
    }

    public Expense execute(ExpenseRequest expenseRequest) {
        String path =storageService.store(expenseRequest.receipt());
        Expense expense= ExpenseRequest.to(expenseRequest);
        expense.setReceiptUrl(path);
        return expenseRepository.save(expense);
    }
}