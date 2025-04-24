package br.com.hyteck.school_control.usecases.student;

import br.com.hyteck.school_control.repositories.StudentRepository;
import br.com.hyteck.school_control.web.dtos.student.StudentResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class FindStudentById {

    private static final Logger logger = LoggerFactory.getLogger(FindStudentById.class);
    private final StudentRepository studentRepository;

    public FindStudentById(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    @Transactional(readOnly = true) // Otimização para operações de leitura
    public Optional<StudentResponse> execute(String id) {
        logger.debug("Buscando estudante com ID: {}", id);
        // Busca no repositório e mapeia para DTO se encontrado
        return studentRepository.findById(id)
                .map(StudentResponse::from);
    }
}