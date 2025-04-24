package br.com.hyteck.school_control.usecases.student;

import br.com.hyteck.school_control.exceptions.DuplicateResourceException;
import br.com.hyteck.school_control.exceptions.ResourceNotFoundException;
import br.com.hyteck.school_control.models.classrooms.Student;
import br.com.hyteck.school_control.models.payments.Responsible;
import br.com.hyteck.school_control.repositories.ResponsibleRepository;
import br.com.hyteck.school_control.repositories.StudentRepository;
import br.com.hyteck.school_control.web.dtos.student.StudentRequest;
import br.com.hyteck.school_control.web.dtos.student.StudentResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.Objects; // Importar Objects para comparação segura

@Service
@Validated
public class UpdateStudent {

    private static final Logger logger = LoggerFactory.getLogger(UpdateStudent.class);
    private final StudentRepository studentRepository;
    private final ResponsibleRepository responsibleRepository;

    public UpdateStudent(StudentRepository studentRepository, ResponsibleRepository responsibleRepository) {
        this.studentRepository = studentRepository;
        this.responsibleRepository = responsibleRepository;
    }

    @Transactional
    public StudentResponse execute(String id, @Valid StudentRequest requestDTO) {
        logger.info("Iniciando atualização do estudante com ID: {}", id);

        // 1. Buscar estudante existente ou lançar exceção
        Student existingStudent = studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Estudante não encontrado com ID: " + id));

        // 2. Verificar duplicidade SE CPF ou Email foram alterados
        checkDuplicates(requestDTO, existingStudent);

        // 3. Buscar o novo Responsável (se o ID mudou ou para garantir que ainda existe)
        Responsible responsible = responsibleRepository.findById(requestDTO.responsibleId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Responsável não encontrado com ID: " + requestDTO.responsibleId()
                ));

        // 4. Atualizar os dados da entidade existente
        existingStudent.setName(requestDTO.name());
        existingStudent.setEmail(requestDTO.email());
        existingStudent.setCpf(requestDTO.cpf());
        existingStudent.setResponsible(responsible); // Atualiza a referência ao responsável

        // 5. Salvar as alterações
        Student updatedStudent = studentRepository.save(existingStudent);
        logger.info("Estudante '{}' atualizado com sucesso. ID: {}", updatedStudent.getName(), updatedStudent.getId());

        // 6. Mapear para DTO de resposta
        return StudentResponse.from(updatedStudent);
    }

    /**
     * Verifica se o novo CPF ou Email já existem para OUTRO estudante.
     */
    private void checkDuplicates(StudentRequest requestDTO, Student existingStudent) {
        // Verifica CPF apenas se foi alterado
//        if (!Objects.equals(requestDTO.cpf(), existingStudent.getCpf())) {
//            studentRepository.findByCpf(requestDTO.cpf()).ifPresent(duplicate -> {
//                // Se encontrou um estudante com o novo CPF, verifica se NÃO é o estudante atual
//                if (!Objects.equals(duplicate.getId(), existingStudent.getId())) {
//                    throw new DuplicateResourceException("CPF já cadastrado para outro estudante: " + requestDTO.cpf());
//                }
//            });
//        }
//
//        // Verifica Email apenas se foi alterado
//        if (!Objects.equals(requestDTO.email(), existingStudent.getEmail())) {
//            studentRepository.findByEmail(requestDTO.email()).ifPresent(duplicate -> {
//                if (!Objects.equals(duplicate.getId(), existingStudent.getId())) {
//                    throw new DuplicateResourceException("Email já cadastrado para outro estudante: " + requestDTO.email());
//                }
//            });
//        }
    }
}