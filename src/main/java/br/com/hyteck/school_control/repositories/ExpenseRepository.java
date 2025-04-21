package br.com.hyteck.school_control.repositories;

import br.com.hyteck.school_control.models.expenses.Expense;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpenseRepository extends JpaRepository<Expense, String> {
}