package br.com.hyteck.school_control.repositories;

import br.com.hyteck.school_control.models.classrooms.Student;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentRepository extends JpaRepository<Student, String> {
}