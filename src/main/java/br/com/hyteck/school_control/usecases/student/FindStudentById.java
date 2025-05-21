package br.com.hyteck.school_control.usecases.student;

import br.com.hyteck.school_control.repositories.StudentRepository;
import br.com.hyteck.school_control.web.dtos.student.StudentResponse;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Service responsible for finding a student by their unique identifier.
 * This use case retrieves a student and maps it to a response DTO if found.
 */
@Service
@AllArgsConstructor
public class FindStudentById {

    private static final Logger logger = LoggerFactory.getLogger(FindStudentById.class);
    private final StudentRepository studentRepository;

    /**
     * Finds a student by their ID and maps it to a StudentResponse DTO if found.
     *
     * @param id the unique identifier of the student
     * @return an Optional containing the StudentResponse if found, or empty if not found
     */
    @Transactional(readOnly = true)
    public Optional<StudentResponse> execute(String id) {
        logger.debug("Searching for student with ID: {}", id);
        // Searches the repository and maps to DTO if found
        return studentRepository.findById(id)
                .map(StudentResponse::from);
    }
}
