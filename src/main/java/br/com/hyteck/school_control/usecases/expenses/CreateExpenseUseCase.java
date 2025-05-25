package br.com.hyteck.school_control.usecases.expenses;

import br.com.hyteck.school_control.models.expenses.Expense;
import br.com.hyteck.school_control.repositories.ExpenseRepository;
import br.com.hyteck.school_control.usecases.storage.StorageService;
import br.com.hyteck.school_control.web.dtos.expenses.ExpenseRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class CreateExpenseUseCase {
    private final ExpenseRepository expenseRepository;

    private final StorageService storageService;
    public CreateExpenseUseCase(ExpenseRepository expenseRepository,
                                @Qualifier("cloudinary") StorageService storageService) {
        this.expenseRepository = expenseRepository;
        this.storageService = storageService;
    }

    public Expense execute(ExpenseRequest expenseRequest) {
        Expense expense= ExpenseRequest.to(expenseRequest);

        if(expenseRequest.receipt() != null){
            String path =storageService.store(expenseRequest.receipt());
            expense.setReceiptUrl(path);
        }


        return expenseRepository.save(expense);
    }
}