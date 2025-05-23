package br.com.hyteck.school_control.web.dtos.classroom;

import br.com.hyteck.school_control.models.classrooms.ClassRoom;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;
import java.time.LocalTime;

// Adicione outras validações conforme necessário (ex: formato do ano)

/**
 * Data Transfer Object (DTO) for receiving data to create or update a {@link ClassRoom}.
 * This record encapsulates the information needed to define a classroom,
 * including its name, school year, and scheduled times.
 *
 * @param name       The name of the classroom (e.g., "Math 101", "History Advanced"). Must not be blank and have at least 2 characters.
 * @param schoolYear The school year to which this classroom pertains, formatted as a four-digit year (e.g., "2024"). Must not be null.
 * @param startTime  The scheduled start time for activities in this classroom. Can be null if not applicable.
 * @param endTime    The scheduled end time for activities in this classroom. Can be null if not applicable.
 */
public record ClassRoomRequest(
        @NotBlank(message = "Classroom name cannot be blank.")
        @Size(min = 2, message = "Classroom name must have at least 2 characters.")
        String name,

        @NotNull(message = "School year cannot be null.")
        @Pattern(regexp = "^\\d{4}$", message = "School year must be a four-digit number.")
        String schoolYear,

        // Consider adding @NotNull if these times are mandatory.
        // Also, ensure startTime is before endTime if both are provided (custom validator or service-level validation).
        LocalTime startTime,
        LocalTime endTime
) {

    /**
     * Static factory method to convert a {@link ClassRoomRequest} DTO to a {@link ClassRoom} entity.
     * This method facilitates the mapping from the data transfer object to the domain model.
     *
     * @param dto The {@link ClassRoomRequest} DTO to convert.
     * @return A {@link ClassRoom} entity populated with data from the DTO.
     *         Note: Fields like capacity or other specific attributes not present in the DTO
     *         will not be set by this method and should be handled separately if needed.
     */
    public static ClassRoom to(ClassRoomRequest dto) {
        // Uses the builder pattern from the ClassRoom entity.
        return ClassRoom.builder()
                .name(dto.name()) // Set classroom name.
                .year(dto.schoolYear()) // Set school year.
                .startTime(dto.startTime()) // Set start time.
                .endTime(dto.endTime()) // Set end time.
                // .capacity(dto.capacity()) // Example: If capacity were in the DTO, it would be mapped here.
                // Other fields specific to the ClassRoom entity that are not in ClassRoomRequest
                // would need to be set elsewhere (e.g., by the service layer with default values or further logic).
                .build(); // Build the ClassRoom object.
    }
}