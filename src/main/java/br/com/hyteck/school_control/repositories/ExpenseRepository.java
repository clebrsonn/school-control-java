package br.com.hyteck.school_control.repositories;

import br.com.hyteck.school_control.models.expenses.Expense;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface ExpenseRepository extends JpaRepository<Expense, String> {
    List<Expense> findByDateBetween(LocalDate startDate, LocalDate endDate);
}