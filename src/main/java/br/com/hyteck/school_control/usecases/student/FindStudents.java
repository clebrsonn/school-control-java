package br.com.hyteck.school_control.usecases.student;

import br.com.hyteck.school_control.repositories.StudentRepository;
import br.com.hyteck.school_control.web.dtos.classroom.ClassRoomResponse;
import br.com.hyteck.school_control.web.dtos.student.StudentResponse;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service responsible for retrieving a paginated list of all students.
 * This use case fetches all students and maps them to response DTOs.
 */
@Service
@AllArgsConstructor
public class FindStudents {

    private static final Logger logger = LoggerFactory.getLogger(FindStudents.class);
    private final StudentRepository studentRepository;


    /**
     * Retrieves a paginated list of all students.
     *
     * @param pageable the pagination information
     * @return a page of StudentResponse DTOs
     */
    @Transactional(readOnly = true)
    public Page<StudentResponse> execute(Pageable pageable) {
        logger.info("Fetching all students paginated: {}", pageable);
        return studentRepository.findAll(pageable)
                .map(StudentResponse::from);
    }
}

