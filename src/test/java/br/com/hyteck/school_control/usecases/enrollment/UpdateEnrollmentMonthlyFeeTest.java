package br.com.hyteck.school_control.usecases.enrollment;

import br.com.hyteck.school_control.exceptions.ResourceNotFoundException;
import br.com.hyteck.school_control.models.classrooms.Enrollment;
import br.com.hyteck.school_control.repositories.EnrollmentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UpdateEnrollmentMonthlyFeeTest {

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @InjectMocks
    private UpdateEnrollmentMonthlyFee updateEnrollmentMonthlyFeeUseCase;

    @Test
    void shouldUpdateMonthlyFeeSuccessfully() {
        String enrollmentId = "enroll-1";
        BigDecimal initialFee = new BigDecimal("500.00");
        BigDecimal newMonthlyFee = new BigDecimal("550.00");

        Enrollment sampleEnrollment = Enrollment.builder()
                .id(enrollmentId)
                .monthlyFee(initialFee)
                .build();

        when(enrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(sampleEnrollment));
        // When save is called, it will return the entity passed to it.
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Enrollment updatedEnrollment = updateEnrollmentMonthlyFeeUseCase.execute(enrollmentId, newMonthlyFee);

        assertNotNull(updatedEnrollment);
        assertEquals(newMonthlyFee, updatedEnrollment.getMonthlyFee(), "The monthly fee should be updated.");

        ArgumentCaptor<Enrollment> enrollmentCaptor = ArgumentCaptor.forClass(Enrollment.class);
        verify(enrollmentRepository).save(enrollmentCaptor.capture());
        assertEquals(newMonthlyFee, enrollmentCaptor.getValue().getMonthlyFee(), "The enrollment saved should have the new fee.");

        verify(enrollmentRepository, times(1)).findById(enrollmentId);
        verify(enrollmentRepository, times(1)).save(sampleEnrollment); // or any(Enrollment.class)
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenEnrollmentDoesNotExist() {
        String enrollmentId = "enroll-non-existent";
        BigDecimal newMonthlyFee = new BigDecimal("600.00");

        when(enrollmentRepository.findById(enrollmentId)).thenReturn(Optional.empty());

        Exception exception = assertThrows(ResourceNotFoundException.class, () -> {
            updateEnrollmentMonthlyFeeUseCase.execute(enrollmentId, newMonthlyFee);
        });

        assertEquals("Enrollment not found with id: " + enrollmentId, exception.getMessage());
        verify(enrollmentRepository, times(1)).findById(enrollmentId);
        verify(enrollmentRepository, never()).save(any(Enrollment.class));
    }
}
