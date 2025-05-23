package br.com.hyteck.school_control.web.dtos.classroom;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Data Transfer Object (DTO) for requesting the creation of a new student enrollment.
 * This record encapsulates all the necessary information to enroll a student into a classroom,
 * including student and classroom identifiers, and associated fees.
 *
 * @param studentId       The unique identifier of the student to be enrolled. Must not be blank.
 * @param classRoomId     The unique identifier of the classroom into which the student will be enrolled.
 *                        (Validation like @NotBlank might be needed if always required).
 * @param classroomName   The name of the classroom, often used for informational purposes or if ID is not available.
 *                        (Consider if this is for creation or just display context).
 * @param enrollmentFee   The fee charged for this enrollment. Can be null or zero if no fee applies.
 * @param monthyFee       The recurring monthly fee for this enrollment. Can be null or zero if no fee applies.
 */
public record EnrollmentRequest(
        @NotBlank(message = "Student ID cannot be blank.")
        String studentId,

        // @NotBlank // This validation is currently commented out. If classRoomId is mandatory, it should be enabled.
        String classRoomId,

        // This field might be redundant if classRoomId is always provided and used for lookup.
        // Or, it could be used if creating a classroom on-the-fly or for user convenience.
        String classroomName,

        @NotNull(message = "Enrollment fee cannot be null. Use 0 if no fee applies.")
        BigDecimal enrollmentFee, // Consider constraints like @PositiveOrZero

        @NotNull(message = "Monthly fee cannot be null. Use 0 if no fee applies.")
        BigDecimal monthyFee // Consider constraints like @PositiveOrZero
) {
    // Records automatically provide a canonical constructor, getters, equals(), hashCode(), and toString().
}