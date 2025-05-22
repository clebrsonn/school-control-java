package br.com.hyteck.school_control.web.dtos.student;

import br.com.hyteck.school_control.models.classrooms.Student;
import br.com.hyteck.school_control.models.payments.Responsible;

import java.time.LocalDateTime;

/**
 * Data Transfer Object (DTO) for returning detailed information about a student.
 * This record provides a representation of a student's data, including their personal details,
 * information about their responsible party, and their current classroom ID if enrolled.
 *
 * @param id              The unique identifier of the student.
 * @param name            The full name of the student.
 * @param email           The student's email address.
 * @param cpf             The student's CPF document number.
 * @param responsibleId   The unique identifier of the student's responsible party.
 * @param responsibleName The name of the student's responsible party.
 * @param classroom       The unique identifier of the student's current classroom.
 *                        This is derived from the student's first active enrollment. May be null if not enrolled.
 * @param createdAt       The timestamp when the student record was created.
 * @param updatedAt       The timestamp when the student record was last updated.
 */
public record StudentResponse(
        String id,
        String name,
        String email,
        String cpf,
        String responsibleId,
        String responsibleName,
        String classroom, // Represents the ID of the current classroom from the first enrollment
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
        /**
         * Static factory method to create a {@link StudentResponse} from a {@link Student} entity.
         * This method handles the conversion from the domain model to the DTO, including
         * extracting information about the responsible party and the student's current classroom.
         *
         * @param student The {@link Student} entity to convert.
         * @return A {@link StudentResponse} populated with data from the Student entity,
         *         or {@code null} if the input student is {@code null}.
         */
        public static StudentResponse from(Student student) {
                // Return null immediately if the source Student entity is null.
                if (student == null) {
                        return null;
                }

                // Safely access responsible party details.
                Responsible responsible = student.getResponsible();
                String respId = responsible != null ? responsible.getId() : null;
                String respName = responsible != null ? responsible.getName() : null;

                // Determine the current classroom ID from the student's enrollments.
                // This assumes the first enrollment in the list is the relevant one,
                // or that enrollments are ordered such that the first is current/primary.
                // More sophisticated logic might be needed if a student can have multiple active/relevant enrollments.
                String currentClassroomId = null;
                if (student.getEnrollments() != null && !student.getEnrollments().isEmpty()) {
                    // Assuming getFirst() gives the most relevant or current enrollment.
                    // And that enrollment has a classroom.
                    var firstEnrollment = student.getEnrollments().getFirst();
                    if (firstEnrollment != null && firstEnrollment.getClassroom() != null) {
                        currentClassroomId = firstEnrollment.getClassroom().getId();
                    }
                }

                // Create and return a new StudentResponse.
                return new StudentResponse(
                        student.getId(), // Student's unique ID.
                        student.getName(), // Student's name.
                        student.getEmail(), // Student's email.
                        student.getCpf(), // Student's CPF.
                        respId, // Responsible party's ID.
                        respName, // Responsible party's name.
                        currentClassroomId, // Current classroom ID.
                        student.getCreatedAt(), // Timestamp of student record creation.
                        student.getUpdatedAt() // Timestamp of last student record update.
                );
        }
}