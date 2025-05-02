// src/main/java/br/com/hyteck/school_control/usecases/student/FindStudentsByResponsibleId.java
package br.com.hyteck.school_control.usecases.student; // Ou um pacote mais apropriado

import br.com.hyteck.school_control.models.classrooms.Student;
import br.com.hyteck.school_control.repositories.StudentRepository; // Importe o repositório de Student
import br.com.hyteck.school_control.web.dtos.student.StudentResponse; // Importe o DTO de resposta do Student
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FindStudentsByResponsibleId {

    private static final Logger logger = LoggerFactory.getLogger(FindStudentsByResponsibleId.class);
    private final StudentRepository studentRepository;
    // Opcional: Injetar ResponsibleRepository para verificar se o responsável existe primeiro
    // private final ResponsibleRepository responsibleRepository;

    public FindStudentsByResponsibleId(StudentRepository studentRepository /*, ResponsibleRepository responsibleRepository */) {
        this.studentRepository = studentRepository;
        // this.responsibleRepository = responsibleRepository;
    }

    @Transactional(readOnly = true) // Bom para operações de leitura
    public Page<StudentResponse> execute(String responsibleId, Pageable page) {
        logger.info("Buscando estudantes para o responsável ID: {}", responsibleId);

        // Opcional: Verificar se o responsável existe antes de buscar os alunos
        // if (!responsibleRepository.existsById(responsibleId)) {
        //     logger.warn("Responsável com ID {} não encontrado.", responsibleId);
        //     // Você pode lançar ResourceNotFoundException aqui ou retornar lista vazia
        //     return Collections.emptyList();
        // }

        Page<Student> students = studentRepository.findByResponsibleId(responsibleId, page);
        if (students.isEmpty()) {
            logger.info("Nenhum estudante encontrado para o responsável ID: {}", responsibleId);
        }

        return students
                .map(StudentResponse::from);
    }
}