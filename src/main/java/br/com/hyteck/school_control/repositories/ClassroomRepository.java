package br.com.hyteck.school_control.repositories;

import br.com.hyteck.school_control.models.classrooms.ClassRoom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClassroomRepository extends JpaRepository<ClassRoom, String> {
    Optional<ClassRoom> findByNameAndYear(String name, String year);
    Optional<ClassRoom> findByName(String name);
    boolean existsByNameAndYear(String name, String year);
}