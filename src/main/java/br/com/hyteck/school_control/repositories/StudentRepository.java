package br.com.hyteck.school_control.repositories;

import br.com.hyteck.school_control.models.classrooms.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudentRepository extends JpaRepository<Student, String> {
    Page<Student> findByResponsibleId(String responsibleId, Pageable pageable);
    java.util.Optional<Student> findByCpf(String cpf);
    java.util.Optional<Student> findByEmail(String email);
}

