package br.com.hyteck.school_control.usecases.enrollment;

import br.com.hyteck.school_control.exceptions.DuplicateResourceException;
import br.com.hyteck.school_control.exceptions.ResourceNotFoundException;
import br.com.hyteck.school_control.models.classrooms.ClassRoom;
import br.com.hyteck.school_control.models.classrooms.Enrollment;
import br.com.hyteck.school_control.models.classrooms.Student;
import br.com.hyteck.school_control.models.payments.Invoice;
import br.com.hyteck.school_control.models.payments.InvoiceStatus;
import br.com.hyteck.school_control.repositories.ClassroomRepository; // Precisa existir
import br.com.hyteck.school_control.repositories.EnrollmentRepository;
import br.com.hyteck.school_control.repositories.StudentRepository;
import br.com.hyteck.school_control.web.dtos.classroom.EnrollmentRequest;
import br.com.hyteck.school_control.web.dtos.classroom.EnrollmentResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;

@Service
@Validated
public class CreateEnrollment {

    private static final Logger logger = LoggerFactory.getLogger(CreateEnrollment.class);
    private final EnrollmentRepository enrollmentRepository;
    private final StudentRepository studentRepository;
    private final ClassroomRepository classRoomRepository; // Injete o repositório de turmas

    public CreateEnrollment(EnrollmentRepository enrollmentRepository,
                            StudentRepository studentRepository,
                            ClassroomRepository classRoomRepository) {
        this.enrollmentRepository = enrollmentRepository;
        this.studentRepository = studentRepository;
        this.classRoomRepository = classRoomRepository;
    }

    @Transactional
    public EnrollmentResponse execute(@Valid EnrollmentRequest requestDTO) {
        logger.info("Iniciando processo de matrícula para estudante {} na turma {}",
                requestDTO.studentId(), requestDTO.classRoomId());

        // 1. Buscar Estudante
        Student student = studentRepository.findById(requestDTO.studentId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Estudante não encontrado com ID: " + requestDTO.studentId()
                ));

        // 2. Buscar Turma
        ClassRoom classRoom = classRoomRepository.findById(requestDTO.classRoomId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Turma não encontrada com ID: " + requestDTO.classRoomId()
                ));

        // 4. Criar a Entidade Enrollment
        Enrollment newEnrollment = Enrollment.builder()
                .student(student)
                .classroom(classRoom)
                .status(Enrollment.Status.ACTIVE)
                .build();

        newEnrollment.validateEnrollmentRules(enrollmentRepository);
        // 5. Salvar a Matrícula
        Enrollment savedEnrollment = enrollmentRepository.save(newEnrollment);
        logger.info("Matrícula criada com sucesso. ID: {}", savedEnrollment.getId());

        if(requestDTO.enrollmentFee() != null && requestDTO.enrollmentFee().signum() > 0){
            //criar matrícula
            createEnrollmentFeeInvoice(savedEnrollment);
        }


        // 6. Mapear para Resposta
        return EnrollmentResponse.from(savedEnrollment);
    }

    private Invoice createEnrollmentFeeInvoice(Enrollment enrollment) {
        return Invoice.builder()
                .enrollment(enrollment)
                .description("Taxa de Matrícula")
                .amount(BigDecimal.valueOf(30))
                .dueDate(LocalDate.now().plusDays(1)) // Exemplo: 7 dias para vencer
                .issueDate(Instant.now().atZone(java.time.ZoneId.systemDefault()).toLocalDate()) // Data de emissão
                .status(InvoiceStatus.PENDING)
                .referenceMonth(YearMonth.now()) // Mês atual
                .build();
    }

}