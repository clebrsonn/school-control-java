package br.com.hyteck.school_control.repositories;

import br.com.hyteck.school_control.models.financials.Account;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, String> {
}