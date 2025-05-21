package br.com.hyteck.school_control.usecases.student;

import br.com.hyteck.school_control.exceptions.DuplicateResourceException;
import br.com.hyteck.school_control.exceptions.ResourceNotFoundException;
import br.com.hyteck.school_control.models.classrooms.Student;
import br.com.hyteck.school_control.models.payments.Responsible;
import br.com.hyteck.school_control.repositories.ResponsibleRepository;
import br.com.hyteck.school_control.repositories.StudentRepository;
import br.com.hyteck.school_control.usecases.enrollment.CreateEnrollment;
import br.com.hyteck.school_control.web.dtos.classroom.EnrollmentRequest;
import br.com.hyteck.school_control.web.dtos.student.StudentRequest;
import br.com.hyteck.school_control.web.dtos.student.StudentResponse;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

/**
 * Service responsible for creating a new student entity.
 * Validates for duplicates, associates the responsible, persists the student, and creates the enrollment.
 */
@Service
@Validated
@AllArgsConstructor
public class CreateStudent {

    private static final Logger logger = LoggerFactory.getLogger(CreateStudent.class);
    private final StudentRepository studentRepository;
    private final ResponsibleRepository responsibleRepository;
    private final CreateEnrollment createEnrollment;


    /**
     * Creates a new student and associates it with a responsible and enrollment.
     *
     * @param requestDTO the student data to create
     * @return the created student response DTO
     * @throws ResourceNotFoundException if the responsible is not found
     * @throws DuplicateResourceException if the CPF or email already exists (if validation is enabled)
     */
    @Transactional // Asegura que la operación sea atómica
    public StudentResponse execute(@Valid StudentRequest requestDTO) {
        logger.info("Starting student creation: {}", requestDTO.name());

        // 1. Verificar duplicados (CPF y Email son buenos candidatos)
//        if (studentRepository.existsByCpf(requestDTO.cpf())) {
//            throw new DuplicateResourceException("Já existe um estudante com o CPF: " + requestDTO.cpf());
//        }
//        if (studentRepository.existsByEmail(requestDTO.email())) {
//            throw new DuplicateResourceException("Já existe um estudante com o email: " + requestDTO.email());
//        }
        Responsible responsible;
        if (requestDTO.responsibleId() == null) {
            responsible = responsibleRepository.findByPhone(requestDTO.responsiblePhone()).orElseThrow(
                    () -> new ResourceNotFoundException(
                            "Responsável não encontrado: " + requestDTO.responsiblePhone()
                    )
            );

        } else {
            responsible = responsibleRepository.findById(requestDTO.responsibleId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Responsável não encontrado: " + requestDTO.responsibleId()
                    ));
        }


        Student studentToSave = Student.builder()
                .name(requestDTO.name())
                .email(requestDTO.email())
                .cpf(requestDTO.cpf())
                .responsible(responsible)
                .build();

        Student savedStudent = studentRepository.save(studentToSave);
        logger.info("Estudante '{}' criado con sucesso. ID: {}", savedStudent.getName(), savedStudent.getId());
        createEnrollment.execute(new EnrollmentRequest(savedStudent.getId(), requestDTO.classroom(), requestDTO.className(),  requestDTO.enrollmentFee(), requestDTO.monthyFee()));
        return StudentResponse.from(savedStudent);
    }
}