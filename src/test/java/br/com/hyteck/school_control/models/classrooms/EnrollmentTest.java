package br.com.hyteck.school_control.models.classrooms;

import br.com.hyteck.school_control.exceptions.DuplicateResourceException;
import br.com.hyteck.school_control.models.classrooms.Enrollment;
import br.com.hyteck.school_control.models.classrooms.Student;
import br.com.hyteck.school_control.models.classrooms.ClassRoom;
import br.com.hyteck.school_control.repositories.EnrollmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EnrollmentTest {

    private EnrollmentRepository enrollmentRepository;
    private Student student;
    private ClassRoom classroom;
    private Enrollment enrollment;

    @BeforeEach
    void setUp() {
        enrollmentRepository = mock(EnrollmentRepository.class);
        student = new Student();
        student.setId("student1");
        student.setName("John Doe");
        classroom = new ClassRoom();
        classroom.setId("class1");
        classroom.setName("Math");
        classroom.setYear("2025");
        enrollment = Enrollment.builder()
                .student(student)
                .classroom(classroom)
                .enrollmentFee(BigDecimal.TEN)
                .monthlyFee(BigDecimal.ONE)
                .build();
    }

    @Test
    void validateEnrollmentRules_shouldPass_whenNoDuplicate() {
        when(enrollmentRepository.existsByStudentIdAndClassroomId("student1", "class1")).thenReturn(false);
        assertDoesNotThrow(() -> enrollment.validateEnrollmentRules(enrollmentRepository));
    }

    @Test
    void validateEnrollmentRules_shouldThrow_whenDuplicateExists() {
        when(enrollmentRepository.existsByStudentIdAndClassroomId("student1", "class1")).thenReturn(true);
        DuplicateResourceException ex = assertThrows(DuplicateResourceException.class, () ->
                enrollment.validateEnrollmentRules(enrollmentRepository));
        assertTrue(ex.getMessage().contains("John Doe"));
        assertTrue(ex.getMessage().contains("Math"));
    }
}


