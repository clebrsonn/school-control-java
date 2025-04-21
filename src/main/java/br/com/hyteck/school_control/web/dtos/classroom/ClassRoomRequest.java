package br.com.hyteck.school_control.web.dtos.classroom;

import br.com.hyteck.school_control.models.classrooms.ClassRoom;
import jakarta.validation.constraints.*;

// Adicione outras validações conforme necessário (ex: formato do ano)

/**
 * DTO para receber os dados de criação ou atualização de uma ClassRoom.
 */
public record ClassRoomRequest(
        @NotBlank
        @Size(min = 2)
        String name,

//        @NotNull
//        @Positive
//        Integer capacity,

        @NotNull
        @Pattern(regexp = "^\\d{4}$")
        String schoolYear // Ou String
        // Adicione outros campos como 'shift' se houver
) {

    public static ClassRoom to(ClassRoomRequest dto) {
        return ClassRoom.builder()
                .name(dto.name())
//                .capacity(dto.capacity())
                .year(dto.schoolYear())
                // Mapear outros campos
                .build();
    }
}