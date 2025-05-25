package br.com.hyteck.school_control.web.dtos.classroom;


import br.com.hyteck.school_control.models.classrooms.ClassRoom;

import java.time.LocalTime;

/**
 * Data Transfer Object (DTO) for returning details of a {@link ClassRoom} to clients.
 * This record provides a structured representation of a classroom's information.
 *
 * @param id         The unique identifier of the classroom.
 * @param name       The name of the classroom (e.g., "Math 101", "History Advanced").
 * @param schoolYear The school year to which this classroom pertains (e.g., "2024").
 * @param startTime  The scheduled start time for activities in this classroom.
 * @param endTime    The scheduled end time for activities in this classroom.
 */
public record ClassRoomResponse(
        String id,
        String name,
        String schoolYear,
        LocalTime startTime,
        LocalTime endTime
) {
    /**
     * Static factory method to create a {@link ClassRoomResponse} from a {@link ClassRoom} entity.
     * This method handles the conversion from the domain model to the DTO.
     *
     * @param classRoom The {@link ClassRoom} entity to convert.
     * @return A {@link ClassRoomResponse} populated with data from the ClassRoom entity,
     *         or {@code null} if the input classRoom is {@code null}.
     */
    public static ClassRoomResponse from(ClassRoom classRoom) {
        // Prevent NullPointerException if the input entity is null.
        if (classRoom == null) {
            return null;
        }
        // Create and return a new ClassRoomResponse, mapping fields from the entity.
        return new ClassRoomResponse(
                classRoom.getId(), // Map the classroom's unique ID.
                classRoom.getName(), // Map the classroom's name.
                classRoom.getYear(), // Map the school year.
                classRoom.getStartTime(), // Map the scheduled start time.
                classRoom.getEndTime() // Map the scheduled end time.
        );
    }
}