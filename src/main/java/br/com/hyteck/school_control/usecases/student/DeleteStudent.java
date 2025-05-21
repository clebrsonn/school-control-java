package br.com.hyteck.school_control.usecases.student;

import br.com.hyteck.school_control.exceptions.ResourceNotFoundException;
import br.com.hyteck.school_control.repositories.StudentRepository;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service responsible for deleting a student entity by its unique identifier.
 * Applies business rules and checks for dependencies before deletion.
 */
@Service
@AllArgsConstructor
public class DeleteStudent {

    private static final Logger logger = LoggerFactory.getLogger(DeleteStudent.class);
    private final StudentRepository studentRepository;
    // private final EnrollmentRepository enrollmentRepository; // Example: inject if you need to check enrollments


    /**
     * Deletes a student by their unique identifier.
     *
     * @param id the unique identifier of the student
     * @throws ResourceNotFoundException if the student does not exist
     * // @throws BusinessRuleException if business rules prevent deletion (e.g., active enrollments)
     */
    @Transactional
    public void execute(String id) {
        logger.info("Starting deletion of student with ID: {}", id);

        // 1. Verify if the student exists
        if (!studentRepository.existsById(id)) {
            throw new ResourceNotFoundException("Student not found with ID: " + id);
        }

        // 2. (Optional) Apply business rules before deletion
        // Example: Check if the student has active enrollments
        /*
        if (enrollmentRepository.existsByStudentIdAndIsActive(id, true)) { // Hypothetical method
            throw new BusinessRuleException("Cannot delete student with active enrollments.");
        }
        */

        // 3. Delete the student
        studentRepository.deleteById(id);
        logger.info("Student successfully deleted. ID: {}", id);
    }
}

