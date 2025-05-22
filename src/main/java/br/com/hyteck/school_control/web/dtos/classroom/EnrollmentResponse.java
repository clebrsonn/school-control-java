package br.com.hyteck.school_control.web.dtos.classroom;

import br.com.hyteck.school_control.models.classrooms.ClassRoom;
import br.com.hyteck.school_control.models.classrooms.Enrollment;
import br.com.hyteck.school_control.models.classrooms.Student;

import java.time.LocalDateTime;

/**
 * Data Transfer Object (DTO) for returning detailed information about a student's enrollment.
 * This record provides a comprehensive view of an enrollment, including student and classroom details,
 * dates, and status.
 *
 * @param id             The unique identifier of the enrollment record itself.
 * @param studentId      The unique identifier of the enrolled student.
 * @param studentName    The name of the enrolled student.
 * @param classRoomId    The unique identifier of the classroom for this enrollment.
 * @param classRoomName  The name of the classroom.
 * @param classRoomYear  The school year of the classroom.
 * @param enrollmentDate The date and time when the enrollment was created or started (maps from {@link Enrollment#getStartDate()}).
 * @param endDate        The date and time when the enrollment ends or is scheduled to end. Can be null for active enrollments.
 * @param status         The current status of the enrollment (e.g., "ACTIVE", "COMPLETED", "CANCELLED").
 */
public record EnrollmentResponse(
        String id,
        String studentId,
        String studentName,
        String classRoomId,
        String classRoomName,
        String classRoomYear,
        LocalDateTime enrollmentDate, // Corresponds to Enrollment.startDate
        LocalDateTime endDate,
        String status
) {
    /**
     * Static factory method to create an {@link EnrollmentResponse} from an {@link Enrollment} entity.
     * This method facilitates the conversion from the domain model to the DTO, handling potential
     * null values within the entity.
     *
     * @param enrollment The {@link Enrollment} entity to convert.
     * @return An {@link EnrollmentResponse} populated with data from the Enrollment entity,
     *         or {@code null} if the input enrollment is {@code null}.
     */
    public static EnrollmentResponse from(Enrollment enrollment) {
        // Return null immediately if the source enrollment entity is null.
        if (enrollment == null) {
            return null;
        }

        // Safely access student and classroom details, handling cases where they might be null.
        Student student = enrollment.getStudent();
        ClassRoom classRoom = enrollment.getClassroom();

        // Create and return a new EnrollmentResponse.
        return new EnrollmentResponse(
                enrollment.getId(), // Enrollment's unique ID.
                student != null ? student.getId() : null, // Student's ID, null if student is not set.
                student != null ? student.getName() : null, // Student's name, null if student is not set.
                classRoom != null ? classRoom.getId() : null, // Classroom's ID, null if classroom is not set.
                classRoom != null ? classRoom.getName() : null, // Classroom's name, null if classroom is not set.
                classRoom != null ? classRoom.getYear() : null, // Classroom's school year, null if classroom is not set.
                enrollment.getStartDate(), // Enrollment start date.
                enrollment.getEndDate(), // Enrollment end date (can be null).
                enrollment.getStatus() != null ? enrollment.getStatus().name() : null // Enrollment status as a string, null if status is not set.
        );
    }
}