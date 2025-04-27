package br.com.hyteck.school_control.repositories;

import br.com.hyteck.school_control.models.payments.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, String> {
    List<Payment> findByPaymentDateBetween(LocalDate startDate, LocalDate endDate);
    List<Payment> findByInvoiceId(String invoiceId);
    
    @Query("SELECT p FROM Payment p WHERE p.invoice.responsible.id = :responsibleId")
    List<Payment> findByResponsibleId(String responsibleId);
}
