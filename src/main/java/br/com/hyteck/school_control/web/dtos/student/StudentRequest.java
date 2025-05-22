package br.com.hyteck.school_control.web.dtos.student;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.br.CPF;

import java.math.BigDecimal;

/**
 * Data Transfer Object (DTO) for requesting the creation or update of a student,
 * and simultaneously handling their initial enrollment details.
 * This record encapsulates student personal data, responsible party identifiers,
 * and classroom enrollment information including fees.
 *
 * @param name             The full name of the student. Must not be blank and max 255 characters.
 * @param email            The student's email address. Must be a valid email format and max 100 characters. Optional.
 * @param cpf              The student's CPF document number. Must be a valid CPF format. Optional.
 * @param responsibleId    The unique identifier of the student's responsible party. Optional if {@code responsiblePhone} is provided.
 * @param responsiblePhone The phone number of the student's responsible party. Optional if {@code responsibleId} is provided.
 *                         Used to find an existing responsible party if ID is not given.
 * @param classroom        The unique identifier of the classroom for the initial enrollment.
 * @param className        The name of the classroom, can be used for context or if ID is not immediately known.
 * @param enrollmentFee    The fee for this initial enrollment. Must not be null.
 * @param monthyFee        The recurring monthly fee for this enrollment. Must not be null.
 */
public record StudentRequest(
        @NotBlank(message = "Student name cannot be blank.")
        @Size(max = 255, message = "Student name must not exceed 255 characters.")
        String name,

        @Email(message = "Student email must be a valid format.")
        @Size(max = 100, message = "Student email must not exceed 100 characters.")
        String email, // Email can be optional for a student

        @CPF(message = "Student CPF must be a valid format.")
        String cpf, // CPF can be optional

        // Responsible party identification: either ID or phone can be used.
        // Service layer typically handles logic to find or create responsible based on these.
        String responsibleId,

        String responsiblePhone, // Consider adding phone format validation if needed

        // Enrollment details
        @NotBlank(message = "Classroom ID for enrollment cannot be blank.") // Assuming classroom ID is mandatory for enrollment
        String classroom, // ID of the classroom

        String className, // Name of the classroom, for context or if creating classroom on the fly

        @NotNull(message = "Enrollment fee cannot be null. Use 0 if no fee.")
        BigDecimal enrollmentFee,

        @NotNull(message = "Monthly fee cannot be null. Use 0 if no fee.")
        BigDecimal monthyFee
) {
    // Records automatically provide a canonical constructor, getters, equals(), hashCode(), and toString().
}