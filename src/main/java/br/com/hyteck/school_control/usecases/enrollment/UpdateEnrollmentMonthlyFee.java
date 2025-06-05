package br.com.hyteck.school_control.usecases.enrollment;

import br.com.hyteck.school_control.exceptions.ResourceNotFoundException;
import br.com.hyteck.school_control.models.classrooms.Enrollment;
import br.com.hyteck.school_control.repositories.EnrollmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class UpdateEnrollmentMonthlyFee {

    private final EnrollmentRepository enrollmentRepository;

    @Transactional
    public Enrollment execute(String enrollmentId, BigDecimal newMonthlyFee) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found with id: " + enrollmentId));

        enrollment.setMonthlyFee(newMonthlyFee);
        // The @Transactional annotation should ensure that changes to the managed 'enrollment' entity
        // are automatically persisted when the transaction commits.
        // Explicitly calling save is also fine and makes the intent clearer.
        return enrollmentRepository.save(enrollment);
    }
}
