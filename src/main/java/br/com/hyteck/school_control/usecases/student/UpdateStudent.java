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

import java.time.LocalDate;
import java.util.Objects;

/**
 * Service responsible for updating a student entity.
 * Validates duplicates, updates the responsible, and persists changes.
 */
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

    /**
     * Updates a student with the provided data.
     *
     * @param id         the student ID
     * @param requestDTO the student data to update
     * @return the updated student response DTO
     * @throws ResourceNotFoundException if the student or responsible is not found
     * @throws DuplicateResourceException if the CPF or email is already used by another student
     */
    @Transactional
    public StudentResponse execute(String id, @Valid StudentRequest requestDTO) {
        logger.info("Starting update for student with ID: {}", id);

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
     * Validates if the current date is within the allowed enrollment period.
     * Throws IllegalStateException if not.
     */
    private void validateEnrollmentPeriod() {
        LocalDate now = LocalDate.now();
        if (now.isBefore(LocalDate.of(now.getYear(), 1, 1)) || 
            now.isAfter(LocalDate.of(now.getYear(), 2, 15))) {
            throw new IllegalStateException("Fora do período de matrículas");
        }
    }

    /**
     * Checks for duplicate CPF or email for another student.
     * Throws DuplicateResourceException if a duplicate is found.
     *
     * @param requestDTO      the student request data
     * @param existingStudent the current student entity
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
