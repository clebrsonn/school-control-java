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
     * Constructs a new CreateStudent service.
     *
     * @param studentRepository     The repository for student data access.
     * @param responsibleRepository The repository for responsible data access.
     * @param createEnrollment      The use case for creating enrollments.
     */
    public CreateStudent(StudentRepository studentRepository, ResponsibleRepository responsibleRepository, CreateEnrollment createEnrollment) {
        this.studentRepository = studentRepository;
        this.responsibleRepository = responsibleRepository;
        this.createEnrollment = createEnrollment;
    }

    /**
     * Creates a new student, associates them with a responsible party, and enrolls them in a class.
     * It first validates if a responsible exists by ID or phone number.
     * Then, it builds and saves the new student. Finally, it creates an enrollment for the student.
     *
     * @param requestDTO the student data to create
     * @return the created student response DTO
     * @throws ResourceNotFoundException if the responsible is not found
     * @throws DuplicateResourceException if the CPF or email already exists (if validation is enabled)
     */
    @Transactional // Asegura que la operación sea atómica
    public StudentResponse execute(@Valid StudentRequest requestDTO) {
        logger.info("Starting student creation: {}", requestDTO.name());

        // Commented out duplicate checks for CPF and Email.
        // These can be re-enabled if strict uniqueness is required before database constraints.
//        if (studentRepository.existsByCpf(requestDTO.cpf())) {
//            throw new DuplicateResourceException("Já existe um estudante com o CPF: " + requestDTO.cpf());
//        }
//        if (studentRepository.existsByEmail(requestDTO.email())) {
//            throw new DuplicateResourceException("Já existe um estudante com o email: " + requestDTO.email());
//        }

        Responsible responsible;
        // Determine how to find the responsible: by ID if provided, otherwise by phone number.
        if (requestDTO.responsibleId() == null) {
            logger.debug("Attempting to find responsible by phone: {}", requestDTO.responsiblePhone());
            responsible = responsibleRepository.findByPhone(requestDTO.responsiblePhone()).orElseThrow(
                    () -> {
                        logger.error("Responsible not found with phone: {}", requestDTO.responsiblePhone());
                        return new ResourceNotFoundException(
                                "Responsável não encontrado com o telefone: " + requestDTO.responsiblePhone()
                        );
                    }
            );
        } else {
            logger.debug("Attempting to find responsible by ID: {}", requestDTO.responsibleId());
            responsible = responsibleRepository.findById(requestDTO.responsibleId())
                    .orElseThrow(() -> {
                        logger.error("Responsible not found with ID: {}", requestDTO.responsibleId());
                        return new ResourceNotFoundException(
                                "Responsável não encontrado com o ID: " + requestDTO.responsibleId()
                        );
                    });
        }
        logger.info("Responsible found: {}", responsible.getName());

        // Build the student entity from the request DTO and the found responsible.
        Student studentToSave = Student.builder()
                .name(requestDTO.name())
                .email(requestDTO.email())
                .cpf(requestDTO.cpf())
                .responsible(responsible) // Associate the fetched or found responsible.
                .build();

        // Persist the new student entity.
        Student savedStudent = studentRepository.save(studentToSave);
        logger.info("Student '{}' created successfully with ID: {}", savedStudent.getName(), savedStudent.getId());

        // Create an enrollment for the newly created student.
        // This links the student to a classroom and sets up fees.
        logger.debug("Creating enrollment for student ID: {} in classroom ID: {}", savedStudent.getId(), requestDTO.classroom());
        createEnrollment.execute(new EnrollmentRequest(
                savedStudent.getId(),
                requestDTO.classroom(),
                requestDTO.className(),
                requestDTO.enrollmentFee(),
                requestDTO.monthyFee()
        ));
        logger.info("Enrollment created for student: {}", savedStudent.getName());

        // Convert the saved student entity to a response DTO.
        return StudentResponse.from(savedStudent);
    }
}