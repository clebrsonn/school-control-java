package br.com.hyteck.school_control.usecases.enrollment;

import br.com.hyteck.school_control.exceptions.ResourceNotFoundException;
import br.com.hyteck.school_control.models.classrooms.ClassRoom;
import br.com.hyteck.school_control.models.classrooms.Enrollment;
import br.com.hyteck.school_control.models.classrooms.Student;
import br.com.hyteck.school_control.models.payments.*;
import br.com.hyteck.school_control.repositories.ClassroomRepository;
import br.com.hyteck.school_control.repositories.EnrollmentRepository;
import br.com.hyteck.school_control.repositories.InvoiceRepository;
import br.com.hyteck.school_control.repositories.StudentRepository;
import br.com.hyteck.school_control.web.dtos.classroom.EnrollmentRequest;
import br.com.hyteck.school_control.web.dtos.classroom.EnrollmentResponse;
import jakarta.validation.Valid;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

@Service
@Validated
@Log4j2
public class CreateEnrollment {

    private final EnrollmentRepository enrollmentRepository;
    private final StudentRepository studentRepository;
    private final ClassroomRepository classRoomRepository;
    private final InvoiceRepository invoiceRepository;

    public CreateEnrollment(EnrollmentRepository enrollmentRepository,
                            StudentRepository studentRepository,
                            ClassroomRepository classRoomRepository, InvoiceRepository invoiceRepository) {
        this.enrollmentRepository = enrollmentRepository;
        this.studentRepository = studentRepository;
        this.classRoomRepository = classRoomRepository;
        this.invoiceRepository = invoiceRepository;
    }

    @Transactional
    public EnrollmentResponse execute(@Valid EnrollmentRequest requestDTO) {

        log.info("Iniciando processo de matrícula para estudante {} na turma {}",
                requestDTO.studentId(), requestDTO.classRoomId());

        Student student = studentRepository.findById(requestDTO.studentId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Estudante não encontrado com ID: " + requestDTO.studentId()
                ));
        ClassRoom classRoom;
        if(requestDTO.classroomName()!= null){
            classRoom = classRoomRepository.findByName(requestDTO.classroomName())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Turma não encontrada com Name: " + requestDTO.classroomName()
                    ));

        }else{
            classRoom = classRoomRepository.findById(requestDTO.classRoomId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Turma não encontrada com ID: " + requestDTO.classRoomId()
                    ));

        }

        Enrollment newEnrollment = Enrollment.builder()
                .student(student)
                .classroom(classRoom)
                .status(Enrollment.Status.ACTIVE)
                .monthlyFee(requestDTO.monthyFee() != null ? requestDTO.monthyFee() : new BigDecimal(110)) // <<< Popula a mensalidade 110) // <<< Popula a mensalidade
                .enrollmentFee(requestDTO.enrollmentFee() != null ? requestDTO.enrollmentFee() : new BigDecimal(30)) // <<< Popula a mensalidade 110) // <<< Popula a mensalidade
                .build();

        newEnrollment.validateEnrollmentRules(enrollmentRepository);
        Enrollment savedEnrollment = enrollmentRepository.save(newEnrollment);
        log.info("Matrícula criada com sucesso. ID: {}", savedEnrollment.getId());

        if (requestDTO.enrollmentFee() != null && requestDTO.enrollmentFee().compareTo(BigDecimal.ZERO) > 0) {
            createAndSaveEnrollmentFeeInvoice(savedEnrollment, requestDTO.enrollmentFee(), student.getResponsible());
        }

        return EnrollmentResponse.from(savedEnrollment);
    }


    private void createAndSaveEnrollmentFeeInvoice(Enrollment enrollment, BigDecimal feeAmount, Responsible responsible) {
        if (responsible == null) {
            log.error("Não foi possível criar a fatura da taxa de matrícula para enrollment {} pois o responsável não foi encontrado.", enrollment.getId());
            // Considerar lançar uma exceção ou uma forma de notificar o problema
            return;
        }

        Invoice feeInvoice = Invoice.builder()
                .responsible(responsible)
                .description("Fatura da Taxa de Matrícula - " + enrollment.getStudent().getName())
                .dueDate(LocalDate.now().plusDays(7)) // Exemplo: 7 dias para vencer
                .issueDate(LocalDate.now())
                .status(InvoiceStatus.PENDING)
                .referenceMonth(YearMonth.now())
                .build();

        InvoiceItem feeItem = InvoiceItem.builder()
                .enrollment(enrollment)
                .description("Taxa de Matrícula - Aluno: " + enrollment.getStudent().getName())
                .amount(feeAmount)
                .type(Types.MATRICULA)
                .build();

        feeInvoice.addItem(feeItem);
        feeInvoice.setAmount(feeInvoice.calculateAmount());

        invoiceRepository.save(feeInvoice); // Salva a fatura (e o item por cascata)
        log.info("Fatura da taxa de matrícula ID {} criada para enrollment {}", feeInvoice.getId(), enrollment.getId());
    }

}