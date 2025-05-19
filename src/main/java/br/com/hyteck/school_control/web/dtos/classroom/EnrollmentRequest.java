package br.com.hyteck.school_control.web.dtos.classroom;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

/**
 * DTO para solicitar a criação de uma matrícula.
 */
public record EnrollmentRequest(
        @NotBlank
        String studentId,

        //@NotBlank
        String classRoomId,

        String classroomName,

        BigDecimal enrollmentFee,

        BigDecimal monthyFee
) {
}