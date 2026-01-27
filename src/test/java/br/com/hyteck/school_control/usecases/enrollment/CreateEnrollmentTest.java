package br.com.hyteck.school_control.usecases.enrollment;

import br.com.hyteck.school_control.models.classrooms.ClassRoom;
import br.com.hyteck.school_control.models.classrooms.Enrollment;
import br.com.hyteck.school_control.models.classrooms.Student;
import br.com.hyteck.school_control.repositories.ClassroomRepository;
import br.com.hyteck.school_control.repositories.EnrollmentRepository;
import br.com.hyteck.school_control.repositories.InvoiceRepository;
import br.com.hyteck.school_control.repositories.StudentRepository;
import br.com.hyteck.school_control.web.dtos.classroom.EnrollmentRequest;
import br.com.hyteck.school_control.web.dtos.classroom.EnrollmentResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateEnrollmentTest {

    @Mock
    private EnrollmentRepository enrollmentRepository;
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private ClassroomRepository classroomRepository;
    @Mock
    private InvoiceRepository invoiceRepository;

    @InjectMocks
    private CreateEnrollment createEnrollment;

    private Student student;
    private ClassRoom classRoom;

    @BeforeEach
    void setUp() {
        student = Student.builder().id("student1").name("Student One").build();
        classRoom = ClassRoom.builder().id("class1").name("Class One").year("2024").build();
    }

    @Test
    void shouldCalculateFeeBasedOnDays() {
        EnrollmentRequest request = new EnrollmentRequest(
                "student1",
                "class1",
                null,
                null,
                null, // Monthly Fee is null, should be calculated
                List.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY),
                LocalTime.of(10, 0),
                LocalTime.of(12, 0)
        );

        when(studentRepository.findById("student1")).thenReturn(Optional.of(student));
        when(classroomRepository.findById("class1")).thenReturn(Optional.of(classRoom));
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        createEnrollment.execute(request);

        ArgumentCaptor<Enrollment> enrollmentCaptor = ArgumentCaptor.forClass(Enrollment.class);
        verify(enrollmentRepository).save(enrollmentCaptor.capture());
        Enrollment savedEnrollment = enrollmentCaptor.getValue();

        assertEquals(new BigDecimal("110.00"), savedEnrollment.getMonthlyFee()); // 2 days * 55
        assertEquals(2, savedEnrollment.getDaysOfWeek().size());
        assertEquals(LocalTime.of(10, 0), savedEnrollment.getStartTime());
        assertEquals(LocalTime.of(12, 0), savedEnrollment.getEndTime());
    }

    @Test
    void shouldCalculateFeeForThreeDays() {
        EnrollmentRequest request = new EnrollmentRequest(
                "student1",
                "class1",
                null,
                null,
                null,
                List.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
                LocalTime.of(10, 0),
                LocalTime.of(12, 0)
        );

        when(studentRepository.findById("student1")).thenReturn(Optional.of(student));
        when(classroomRepository.findById("class1")).thenReturn(Optional.of(classRoom));
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        createEnrollment.execute(request);

        ArgumentCaptor<Enrollment> enrollmentCaptor = ArgumentCaptor.forClass(Enrollment.class);
        verify(enrollmentRepository).save(enrollmentCaptor.capture());
        Enrollment savedEnrollment = enrollmentCaptor.getValue();

        assertEquals(new BigDecimal("165.00"), savedEnrollment.getMonthlyFee()); // 3 days * 55
    }

    @Test
    void shouldUseDefaultFeeWhenNoDaysProvided() {
        EnrollmentRequest request = new EnrollmentRequest(
                "student1",
                "class1",
                null,
                null,
                null,
                null, // No days
                null,
                null
        );

        when(studentRepository.findById("student1")).thenReturn(Optional.of(student));
        when(classroomRepository.findById("class1")).thenReturn(Optional.of(classRoom));
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        createEnrollment.execute(request);

        ArgumentCaptor<Enrollment> enrollmentCaptor = ArgumentCaptor.forClass(Enrollment.class);
        verify(enrollmentRepository).save(enrollmentCaptor.capture());
        Enrollment savedEnrollment = enrollmentCaptor.getValue();

        assertEquals(new BigDecimal("110.00"), savedEnrollment.getMonthlyFee()); // Default
    }

    @Test
    void shouldUseProvidedFee() {
        BigDecimal customFee = new BigDecimal("200.00");
        EnrollmentRequest request = new EnrollmentRequest(
                "student1",
                "class1",
                null,
                null,
                customFee, // Provided
                List.of(DayOfWeek.MONDAY),
                null,
                null
        );

        when(studentRepository.findById("student1")).thenReturn(Optional.of(student));
        when(classroomRepository.findById("class1")).thenReturn(Optional.of(classRoom));
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        createEnrollment.execute(request);

        ArgumentCaptor<Enrollment> enrollmentCaptor = ArgumentCaptor.forClass(Enrollment.class);
        verify(enrollmentRepository).save(enrollmentCaptor.capture());
        Enrollment savedEnrollment = enrollmentCaptor.getValue();

        assertEquals(customFee, savedEnrollment.getMonthlyFee());
    }
}
