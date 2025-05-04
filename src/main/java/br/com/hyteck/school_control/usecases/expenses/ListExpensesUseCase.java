package br.com.hyteck.school_control.usecases.expenses;

import br.com.hyteck.school_control.models.expenses.Expense;
import br.com.hyteck.school_control.repositories.ExpenseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ListExpensesUseCase {
    private final ExpenseRepository expenseRepository;

    public ListExpensesUseCase(ExpenseRepository expenseRepository) {
        this.expenseRepository = expenseRepository;
    }

    public Page<Expense> execute(Pageable pageable) {
        return expenseRepository.findAll(pageable);
    }
}