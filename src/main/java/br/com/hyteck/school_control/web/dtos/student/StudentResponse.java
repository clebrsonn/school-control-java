package br.com.hyteck.school_control.web.dtos.student;

import br.com.hyteck.school_control.models.classrooms.Student;
import br.com.hyteck.school_control.models.payments.Responsible; // Importar Responsible

import java.time.LocalDateTime;

public record StudentResponse(
        String id,
        String name,
        String email,
        String cpf,
        String responsibleId,
        String responsibleName,
        String classroom,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
        public static StudentResponse from(Student student) {
                if (student == null) {
                        return null;
                }
                Responsible responsible = student.getResponsible(); // Obtener el responsable asociado
                return new StudentResponse(
                        student.getId(),
                        student.getName(),
                        student.getEmail(),
                        student.getCpf(),
                        responsible != null ? responsible.getId() : null,
                        responsible != null ? responsible.getName() : null,
                        student.getEnrollments() != null && !student.getEnrollments().isEmpty() ?student.getEnrollments().getFirst().getClassroom().getId() : null,
                        student.getCreatedAt(),
                        student.getUpdatedAt()
                );
        }
}