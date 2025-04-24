package br.com.hyteck.school_control.usecases.student;

import br.com.hyteck.school_control.repositories.StudentRepository;
import br.com.hyteck.school_control.web.dtos.classroom.ClassRoomResponse;
import br.com.hyteck.school_control.web.dtos.student.StudentResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FindStudents {

    private static final Logger logger = LoggerFactory.getLogger(FindStudents.class);
    private final StudentRepository studentRepository;

    public FindStudents(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    @Transactional(readOnly = true)
    public Page<StudentResponse> execute(Pageable pageable) {
        logger.info("Buscando todas as ClassRooms paginadas: {}", pageable);
        return studentRepository.findAll(pageable)
                .map(StudentResponse::from);
    }
}