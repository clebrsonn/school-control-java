package br.com.hyteck.school_control.usecases.enrollment;

import br.com.hyteck.school_control.events.InvoiceCreatedEvent;
import br.com.hyteck.school_control.models.classrooms.ClassRoom;
import br.com.hyteck.school_control.models.classrooms.Enrollment;
import br.com.hyteck.school_control.models.classrooms.Student;
import br.com.hyteck.school_control.models.payments.Invoice;
import br.com.hyteck.school_control.models.payments.Responsible;
import br.com.hyteck.school_control.repositories.ClassroomRepository;
import br.com.hyteck.school_control.repositories.EnrollmentRepository;
import br.com.hyteck.school_control.repositories.InvoiceRepository;
import br.com.hyteck.school_control.repositories.StudentRepository;
import br.com.hyteck.school_control.web.dtos.classroom.EnrollmentRequest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class CreateEnrollmentTest {

    @Mock private EnrollmentRepository enrollmentRepository;
    @Mock private StudentRepository studentRepository;
    @Mock private ClassroomRepository classroomRepository;
    @Mock private InvoiceRepository invoiceRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks private CreateEnrollment createEnrollment;

    @Test
    void execute_whenEnrollmentFeeExists_publishesInvoiceCreatedEvent() {
        // Arrange
        String studentId = UUID.randomUUID().toString();
        String classroomId = UUID.randomUUID().toString();
        String responsibleId = UUID.randomUUID().toString();
        Responsible responsible = Responsible.builder().id(responsibleId).name("Test Responsible").build();
        Student student = Student.builder().id(studentId).name("Test Student").responsible(responsible).build();
        ClassRoom classRoom = ClassRoom.builder().id(classroomId).name("Test Class").year(String.valueOf(YearMonth.now().getYear())).build();
        
        // EnrollmentRequest: studentId, classRoomId, classroomName, enrollmentFee, monthlyFee
        EnrollmentRequest request = new EnrollmentRequest(studentId, classroomId, null, BigDecimal.valueOf(50), BigDecimal.valueOf(200)); // 50 for enrollment fee

        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(classroomRepository.findById(classroomId)).thenReturn(Optional.of(classRoom));
        
        // Mock enrollment save to return the passed enrollment
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(invocation -> {
            Enrollment e = invocation.getArgument(0);
            // Simulate JPA behavior of assigning an ID if it's not set, or just return it
            if (e.getId() == null) {
                e.setId(UUID.randomUUID().toString()); 
            }
            return e;
        });
        
        Invoice savedInvoice = Invoice.builder().id(UUID.randomUUID().toString()).amount(BigDecimal.valueOf(50)).build();
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(savedInvoice);

        // Act
        createEnrollment.execute(request);

        // Assert
        ArgumentCaptor<InvoiceCreatedEvent> eventCaptor = ArgumentCaptor.forClass(InvoiceCreatedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        
        InvoiceCreatedEvent publishedEvent = eventCaptor.getValue();
        assertNotNull(publishedEvent);
        assertEquals(savedInvoice, publishedEvent.getInvoice());
        assertEquals(createEnrollment, publishedEvent.getSource()); 
    }

    @Test
    void execute_whenEnrollmentFeeIsZero_doesNotPublishEvent() {
        // Arrange
        String studentId = UUID.randomUUID().toString();
        String classroomId = UUID.randomUUID().toString();
        String responsibleId = UUID.randomUUID().toString();
        Responsible responsible = Responsible.builder().id(responsibleId).name("Test Responsible").build();
        Student student = Student.builder().id(studentId).name("Test Student").responsible(responsible).build();
        ClassRoom classRoom = ClassRoom.builder().id(classroomId).name("Test Class").year(String.valueOf(YearMonth.now().getYear())).build();
        
        EnrollmentRequest request = new EnrollmentRequest(studentId, classroomId, null, BigDecimal.ZERO, BigDecimal.valueOf(200)); // Zero enrollment fee

        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(classroomRepository.findById(classroomId)).thenReturn(Optional.of(classRoom));
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(invocation -> {
            Enrollment e = invocation.getArgument(0);
             if (e.getId() == null) {
                e.setId(UUID.randomUUID().toString());
            }
            return e;
        });

        // Act
        createEnrollment.execute(request);

        // Assert
        verify(eventPublisher, never()).publishEvent(any(InvoiceCreatedEvent.class));
        verify(invoiceRepository, never()).save(any(Invoice.class)); // Also verify invoice is not saved
    }
    
    @Test
    void execute_whenEnrollmentFeeIsNull_doesNotPublishEvent() {
        // Arrange
        String studentId = UUID.randomUUID().toString();
        String classroomId = UUID.randomUUID().toString();
        String responsibleId = UUID.randomUUID().toString();
        Responsible responsible = Responsible.builder().id(responsibleId).name("Test Responsible").build();
        Student student = Student.builder().id(studentId).name("Test Student").responsible(responsible).build();
        ClassRoom classRoom = ClassRoom.builder().id(classroomId).name("Test Class").year(String.valueOf(YearMonth.now().getYear())).build();

        EnrollmentRequest request = new EnrollmentRequest(studentId, classroomId, null, null, BigDecimal.valueOf(200)); // Null enrollment fee

        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(classroomRepository.findById(classroomId)).thenReturn(Optional.of(classRoom));
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(invocation -> {
            Enrollment e = invocation.getArgument(0);
            if (e.getId() == null) {
                e.setId(UUID.randomUUID().toString());
            }
            return e;
        });
        // Act
        createEnrollment.execute(request);

        // Assert
        verify(eventPublisher, never()).publishEvent(any(InvoiceCreatedEvent.class));
        verify(invoiceRepository, never()).save(any(Invoice.class)); // Also verify invoice is not saved
    }
}
