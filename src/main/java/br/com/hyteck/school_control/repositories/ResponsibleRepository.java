package br.com.hyteck.school_control.repositories;

import br.com.hyteck.school_control.models.payments.Responsible;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface ResponsibleRepository extends JpaRepository<Responsible, String> {
    Optional<Responsible> findByEmail(String email);
    Optional<Responsible> findByPhone(String phone);


    @Query("select new Responsible( r.id, r.name, r.email, r.phone) from Responsible r")
    @Override
    Page<Responsible> findAll(Pageable pageable);

}
