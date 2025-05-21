// src/main/java/br/com/hyteck/school_control/usecases/student/FindStudentsByResponsibleId.java
package br.com.hyteck.school_control.usecases.student; // Ou um pacote mais apropriado

import br.com.hyteck.school_control.models.classrooms.Student;
import br.com.hyteck.school_control.repositories.StudentRepository; // Importe o repositório de Student
import br.com.hyteck.school_control.web.dtos.student.StudentResponse; // Importe o DTO de resposta do Student
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service responsible for retrieving a paginated list of students by responsible ID.
 * This use case fetches students associated with a given responsible and maps them to response DTOs.
 */
@Service
@AllArgsConstructor
public class FindStudentsByResponsibleId {

    private static final Logger logger = LoggerFactory.getLogger(FindStudentsByResponsibleId.class);
    private final StudentRepository studentRepository;

    /**
     * Retrieves a paginated list of students for a given responsible ID.
     *
     * @param responsibleId the unique identifier of the responsible
     * @param page the pagination information
     * @return a page of StudentResponse DTOs
     */
    @Transactional(readOnly = true)
    public Page<StudentResponse> execute(String responsibleId, Pageable page) {
        logger.info("Fetching students for responsible ID: {}", responsibleId);
        // Optionally check if responsible exists before fetching students
        // if (!responsibleRepository.existsById(responsibleId)) {
        //     logger.warn("Responsible with ID {} not found.", responsibleId);
        //     return Collections.emptyList();
        // }
        Page<Student> students = studentRepository.findByResponsibleId(responsibleId, page);
        if (students.isEmpty()) {
            logger.info("No students found for responsible ID: {}", responsibleId);
        }
        return students.map(StudentResponse::from);
    }
}

