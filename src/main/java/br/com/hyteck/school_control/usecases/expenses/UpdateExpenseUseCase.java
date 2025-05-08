package br.com.hyteck.school_control.usecases.expenses;

import br.com.hyteck.school_control.exceptions.ResourceNotFoundException;
import br.com.hyteck.school_control.models.expenses.Expense;
import br.com.hyteck.school_control.repositories.ExpenseRepository;
import br.com.hyteck.school_control.usecases.storage.StorageService;
import br.com.hyteck.school_control.web.dtos.expenses.ExpenseRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class UpdateExpenseUseCase {
    private static final Logger logger = LoggerFactory.getLogger(UpdateExpenseUseCase.class);
    private final ExpenseRepository expenseRepository;
    private final StorageService storageService;
    public UpdateExpenseUseCase(ExpenseRepository expenseRepository,
                                @Qualifier(value = "cloudinary") StorageService storageService) {
        this.expenseRepository = expenseRepository;
        this.storageService = storageService;
    }

    public Expense execute(String id, ExpenseRequest expenseRequest) {

        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("Expense não encontrado para atualização. ID: {}", id);
                    return new ResourceNotFoundException("Expense não encontrado com ID: " + id);
                });

        String path= storageService.store(expenseRequest.receipt());

        expense.setDate(expenseRequest.date());
        expense.setValue(expenseRequest.value());
        expense.setDescription(expenseRequest.description());
        expense.setReceiptUrl(path);
        return expenseRepository.save(  expense);
    }
}