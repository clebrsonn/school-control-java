package br.com.hyteck.school_control.usecases.student;

import br.com.hyteck.school_control.exceptions.DuplicateResourceException;
import br.com.hyteck.school_control.exceptions.ResourceNotFoundException;
import br.com.hyteck.school_control.models.classrooms.Student;
import br.com.hyteck.school_control.models.payments.Responsible;
import br.com.hyteck.school_control.repositories.ResponsibleRepository;
import br.com.hyteck.school_control.repositories.StudentRepository;
import br.com.hyteck.school_control.web.dtos.student.StudentRequest;
import br.com.hyteck.school_control.web.dtos.student.StudentResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class UpdateStudentTest {

    private StudentRepository studentRepository;
    private ResponsibleRepository responsibleRepository;
    private UpdateStudent updateStudent;

    @BeforeEach
    void setUp() {
        studentRepository = mock(StudentRepository.class);
        responsibleRepository = mock(ResponsibleRepository.class);
        updateStudent = new UpdateStudent(studentRepository, responsibleRepository);
    }

    @Test
    void execute_shouldUpdateStudent_whenDataIsValid() {
        Student existing = new Student();
        existing.setId("student1");
        existing.setCpf("12345678901");
        existing.setEmail("old@email.com");
        Responsible responsible = new Responsible();
        responsible.setId("resp1");
        when(studentRepository.findById("student1")).thenReturn(Optional.of(existing));
        when(responsibleRepository.findById("resp1")).thenReturn(Optional.of(responsible));
        when(studentRepository.save(any(Student.class))).thenAnswer(inv -> inv.getArgument(0));
        StudentRequest request = new StudentRequest("John Updated", "new@email.com", "12345678901", "resp1", null, null, null, null, null);
        StudentResponse response = updateStudent.execute("student1", request);
        assertEquals("John Updated", response.name());
        assertEquals("new@email.com", response.email());
    }

    @Test
    void execute_shouldThrowResourceNotFoundException_whenStudentNotFound() {
        when(studentRepository.findById("student1")).thenReturn(Optional.empty());
        StudentRequest request = new StudentRequest("John Updated", "new@email.com", "12345678901", "resp1", null, null, null, null, null);
        assertThrows(ResourceNotFoundException.class, () -> updateStudent.execute("student1", request));
    }

    @Test
    void execute_shouldThrowResourceNotFoundException_whenResponsibleNotFound() {
        Student existing = new Student();
        existing.setId("student1");
        when(studentRepository.findById("student1")).thenReturn(Optional.of(existing));
        when(responsibleRepository.findById("resp1")).thenReturn(Optional.empty());
        StudentRequest request = new StudentRequest("John Updated", "new@email.com", "12345678901", "resp1", null, null, null, null, null);
        assertThrows(ResourceNotFoundException.class, () -> updateStudent.execute("student1", request));
    }

    @Test
    void execute_shouldThrowDuplicateResourceException_whenCpfDuplicated() {
        Student existing = new Student();
        existing.setId("student1");
        existing.setCpf("oldcpf");
        when(studentRepository.findById("student1")).thenReturn(Optional.of(existing));
        when(responsibleRepository.findById("resp1")).thenReturn(Optional.of(new Responsible()));
        // Simula duplicidade de CPF
        Student duplicate = new Student();
        duplicate.setId("student2");
        when(studentRepository.findByCpf("12345678901")).thenReturn(Optional.of(duplicate));
        StudentRequest request = new StudentRequest("John Updated", "new@email.com", "12345678901", "resp1", null, null, null, null, null);
        assertThrows(DuplicateResourceException.class, () -> updateStudent.execute("student1", request));
    }

    @Test
    void execute_shouldThrowDuplicateResourceException_whenEmailDuplicated() {
        Student existing = new Student();
        existing.setId("student1");
        existing.setEmail("old@email.com");
        when(studentRepository.findById("student1")).thenReturn(Optional.of(existing));
        when(responsibleRepository.findById("resp1")).thenReturn(Optional.of(new Responsible()));
        // Simula duplicidade de email
        Student duplicate = new Student();
        duplicate.setId("student2");
        when(studentRepository.findByEmail("new@email.com")).thenReturn(Optional.of(duplicate));
        StudentRequest request = new StudentRequest("John Updated", "new@email.com", "12345678901", "resp1", null, null, null, null, null);
        assertThrows(DuplicateResourceException.class, () -> updateStudent.execute("student1", request));
    }
}

