package br.com.hyteck.school_control.repositories;

import br.com.hyteck.school_control.models.classrooms.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnrollmentRepository extends JpaRepository<Enrollment, String> {
    boolean existsByClassroomId(String classRoomId);
}