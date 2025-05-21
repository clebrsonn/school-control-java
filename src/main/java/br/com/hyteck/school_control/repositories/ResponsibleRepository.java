package br.com.hyteck.school_control.repositories;

import br.com.hyteck.school_control.models.expenses.Expense;
import br.com.hyteck.school_control.models.payments.Responsible;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ResponsibleRepository extends JpaRepository<Responsible, String> {
    Optional<Responsible> findByEmail(String email);
    Optional<Responsible> findByPhone(String phone);
}
