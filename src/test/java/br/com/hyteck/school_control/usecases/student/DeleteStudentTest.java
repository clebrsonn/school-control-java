package br.com.hyteck.school_control.usecases.student;

import br.com.hyteck.school_control.exceptions.ResourceNotFoundException;
import br.com.hyteck.school_control.repositories.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DeleteStudentTest {

    private StudentRepository studentRepository;
    private DeleteStudent deleteStudent;

    @BeforeEach
    void setUp() {
        studentRepository = mock(StudentRepository.class);
        deleteStudent = new DeleteStudent(studentRepository);
    }

    @Test
    void execute_shouldDeleteStudent_whenExists() {
        when(studentRepository.existsById("student1")).thenReturn(true);
        doNothing().when(studentRepository).deleteById("student1");
        assertDoesNotThrow(() -> deleteStudent.execute("student1"));
        verify(studentRepository).deleteById("student1");
    }

    @Test
    void execute_shouldThrowResourceNotFoundException_whenStudentNotFound() {
        when(studentRepository.existsById("student1")).thenReturn(false);
        assertThrows(ResourceNotFoundException.class, () -> deleteStudent.execute("student1"));
    }
}

