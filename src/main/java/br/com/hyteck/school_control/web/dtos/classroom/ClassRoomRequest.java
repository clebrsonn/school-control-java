package br.com.hyteck.school_control.web.dtos.classroom;

import br.com.hyteck.school_control.models.classrooms.ClassRoom;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;
import java.time.LocalTime;

// Adicione outras validações conforme necessário (ex: formato do ano)

/**
 * DTO para receber os dados de criação ou atualização de uma ClassRoom.
 */
public record ClassRoomRequest(
        @NotBlank
        @Size(min = 2)
        String name,

        @NotNull
        @Pattern(regexp = "^\\d{4}$")
        String schoolYear,
        LocalTime startTime,
        LocalTime endTime

) {

    public static ClassRoom to(ClassRoomRequest dto) {
        return ClassRoom.builder()
                .name(dto.name())
                .endTime(dto.endTime())
                .startTime(dto.startTime())

//                .capacity(dto.capacity())
                .year(dto.schoolYear())
                // Mapear outros campos
                .build();
    }
}