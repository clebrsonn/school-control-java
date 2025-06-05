package br.com.hyteck.school_control.web.dtos.classroom;

import br.com.hyteck.school_control.models.classrooms.ClassRoom;
import br.com.hyteck.school_control.models.classrooms.Enrollment;
import br.com.hyteck.school_control.models.classrooms.ClassRoom;
import br.com.hyteck.school_control.models.classrooms.Enrollment;
import br.com.hyteck.school_control.models.classrooms.Student;

import java.math.BigDecimal; // Added
import java.time.LocalDateTime;

/**
 * DTO para retornar informações sobre uma matrícula.
 */
public record EnrollmentResponse(
        String id, // ID da matrícula
        String studentId,
        String studentName,
        String classRoomId,
        String classRoomName,
        String classRoomYear,
        BigDecimal monthlyFee, // Added
        LocalDateTime enrollmentDate,
        LocalDateTime endDate,
        String status
) {
    public static EnrollmentResponse from(Enrollment enrollment) {
        if (enrollment == null) {
            return null;
        }
        Student student = enrollment.getStudent();
        ClassRoom classRoom = enrollment.getClassroom();
        return new EnrollmentResponse(
                enrollment.getId(),
                student != null ? student.getId() : null,
                student != null ? student.getName() : null,
                classRoom != null ? classRoom.getId() : null,
                classRoom != null ? classRoom.getName() : null,
                classRoom != null ? classRoom.getYear() : null,
                enrollment.getMonthlyFee(), // Added
                enrollment.getStartDate(),
                enrollment.getEndDate(),
                enrollment.getStatus().name()
        );
    }
}