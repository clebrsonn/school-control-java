package br.com.hyteck.school_control.repositories;

import br.com.hyteck.school_control.models.payments.Discount;
import br.com.hyteck.school_control.models.payments.Types;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface DiscountRepository extends JpaRepository<Discount, String> {
  @Query("SELECT d FROM Discount d WHERE d.type = :type AND d.validateAt > CURRENT_DATE")
  Optional<Discount> findByTypeAndValidAtBeforeToday(Types type);
}