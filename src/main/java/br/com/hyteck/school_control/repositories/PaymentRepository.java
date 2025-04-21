package br.com.hyteck.school_control.repositories;

import br.com.hyteck.school_control.models.payments.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, String> {
}
