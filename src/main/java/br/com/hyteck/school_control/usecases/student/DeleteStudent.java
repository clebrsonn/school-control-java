package br.com.hyteck.school_control.usecases.student;

import br.com.hyteck.school_control.exceptions.ResourceNotFoundException;
import br.com.hyteck.school_control.repositories.StudentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeleteStudent {

    private static final Logger logger = LoggerFactory.getLogger(DeleteStudent.class);
    private final StudentRepository studentRepository;
    // private final EnrollmentRepository enrollmentRepository; // Exemplo

    public DeleteStudent(StudentRepository studentRepository /*, EnrollmentRepository enrollmentRepository */) {
        this.studentRepository = studentRepository;
        // this.enrollmentRepository = enrollmentRepository;
    }

    @Transactional
    public void execute(String id) {
        logger.info("Iniciando exclusão do estudante com ID: {}", id);

        // 1. Verificar se o estudante existe
        if (!studentRepository.existsById(id)) {
            throw new ResourceNotFoundException("Estudante não encontrado com ID: " + id);
        }

        // 2. (Opcional) Aplicar regras de negócio antes de deletar
        // Exemplo: Verificar se o estudante tem matrículas ativas
        /*
        if (enrollmentRepository.existsByStudentIdAndIsActive(id, true)) { // Método hipotético
            throw new BusinessRuleException("Não é possível excluir estudante com matrículas ativas.");
        }
        */

        // 3. Deletar o estudante
        studentRepository.deleteById(id);
        logger.info("Estudante excluído com sucesso. ID: {}", id);
    }
}