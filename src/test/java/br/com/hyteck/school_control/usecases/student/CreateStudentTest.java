package br.com.hyteck.school_control.usecases.student;

import br.com.hyteck.school_control.exceptions.DuplicateResourceException;
import br.com.hyteck.school_control.exceptions.ResourceNotFoundException;
import br.com.hyteck.school_control.models.classrooms.Student;
import br.com.hyteck.school_control.models.payments.Responsible;
import br.com.hyteck.school_control.repositories.ResponsibleRepository;
import br.com.hyteck.school_control.repositories.StudentRepository;
import br.com.hyteck.school_control.usecases.enrollment.CreateEnrollment;
import br.com.hyteck.school_control.web.dtos.classroom.EnrollmentRequest;
import br.com.hyteck.school_control.web.dtos.student.StudentRequest;
import br.com.hyteck.school_control.web.dtos.student.StudentResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CreateStudentTest {

    private StudentRepository studentRepository;
    private ResponsibleRepository responsibleRepository;
    private CreateEnrollment createEnrollment;
    private CreateStudent createStudent;

    @BeforeEach
    void setUp() {
        studentRepository = mock(StudentRepository.class);
        responsibleRepository = mock(ResponsibleRepository.class);
        createEnrollment = mock(CreateEnrollment.class);
        createStudent = new CreateStudent(studentRepository, responsibleRepository, createEnrollment);
    }

    @Test
    void execute_shouldCreateStudent_whenDataIsValid() {
        StudentRequest request = new StudentRequest("John Doe", "john@email.com", "12345678901", "resp1", null, "class1", "Class A", null, null);
        Responsible responsible = new Responsible();
        responsible.setId("resp1");
        when(responsibleRepository.findById("resp1")).thenReturn(Optional.of(responsible));
        when(studentRepository.save(any(Student.class))).thenAnswer(inv -> {
            Student s = inv.getArgument(0);
            s.setId("student1");
            return s;
        });
        StudentResponse response = createStudent.execute(request);
        assertEquals("John Doe", response.name());
        assertEquals("student1", response.id());
        verify(createEnrollment).execute(any(EnrollmentRequest.class));
    }
}

