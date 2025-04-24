package br.com.hyteck.school_control.usecases.student;

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

@Service
@Validated
public class CreateStudent {

    private static final Logger logger = LoggerFactory.getLogger(CreateStudent.class);
    private final StudentRepository studentRepository;
    private final ResponsibleRepository responsibleRepository;

    public CreateStudent(StudentRepository studentRepository, ResponsibleRepository responsibleRepository) {
        this.studentRepository = studentRepository;
        this.responsibleRepository = responsibleRepository;
    }

    @Transactional // Asegura que la operación sea atómica
    public StudentResponse execute(@Valid StudentRequest requestDTO) {
        logger.info("Iniciando creación de estudiante: {}", requestDTO.name());

        // 1. Verificar duplicados (CPF y Email son buenos candidatos)
//        if (studentRepository.existsByCpf(requestDTO.cpf())) {
//            throw new DuplicateResourceException("Já existe um estudante com o CPF: " + requestDTO.cpf());
//        }
//        if (studentRepository.existsByEmail(requestDTO.email())) {
//            throw new DuplicateResourceException("Já existe um estudante com o email: " + requestDTO.email());
//        }

        // 2. Buscar el Responsable existente por ID
        Responsible responsible = responsibleRepository.findById(requestDTO.responsibleId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Responsável não encontrado: " + requestDTO.responsibleId()
                ));

        // 3. Mapear DTO a Entidad Student
        Student studentToSave = Student.builder()
                .name(requestDTO.name())
                .email(requestDTO.email())
                .cpf(requestDTO.cpf())
                .responsible(responsible) // Asignar el responsable encontrado
                // Los enrollments se inicializan vacíos por defecto en la entidad
                .build();

        // 4. Persistir el nuevo estudiante
        Student savedStudent = studentRepository.save(studentToSave);
        logger.info("Estudante '{}' criado con sucesso. ID: {}", savedStudent.getName(), savedStudent.getId());

        // 5. Mapear la entidad guardada a DTO de Respuesta
        return StudentResponse.from(savedStudent);
    }
}