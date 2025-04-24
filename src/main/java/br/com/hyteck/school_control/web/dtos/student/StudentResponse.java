package br.com.hyteck.school_control.web.dtos.student;

import br.com.hyteck.school_control.models.classrooms.Student;
import br.com.hyteck.school_control.models.payments.Responsible; // Importar Responsible

import java.time.LocalDateTime;

public record StudentResponse(
        String id,
        String name,
        String email,
        String cpf,
        String responsibleId, // ID del responsable
        String responsibleName, // Nombre del responsable para conveniencia
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
                        responsible != null ? responsible.getId() : null, // Obtener ID del responsable
                        responsible != null ? responsible.getName() : null, // Obtener Nombre del responsable
                        student.getCreatedAt(),
                        student.getUpdatedAt()
                );
        }
}