package br.com.hyteck.school_control.web.dtos.classroom;


import br.com.hyteck.school_control.models.classrooms.ClassRoom;

/**
 * DTO para retornar os dados de uma ClassRoom.
 */
public record ClassRoomResponse(
        String id,
        String name,
//        Integer capacity,
        String schoolYear // Ou String
        // Adicione outros campos
) {
    /**
     * Método factory para converter uma entidade ClassRoom em um DTO de resposta.
     */
    public static ClassRoomResponse from(ClassRoom classRoom) {
        if (classRoom == null) {
            return null;
        }
        return new ClassRoomResponse(
                classRoom.getId(),
                classRoom.getName(),
//                classRoom.getCapacity(),
                classRoom.getYear()
                // Mapeie outros campos
        );
    }
}