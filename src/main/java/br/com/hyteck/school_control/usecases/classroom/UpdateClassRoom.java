package br.com.hyteck.school_control.usecases.classroom;

import br.com.hyteck.school_control.web.dtos.classroom.ClassRoomRequest;
import br.com.hyteck.school_control.web.dtos.classroom.ClassRoomResponse;
import br.com.hyteck.school_control.exceptions.DuplicateResourceException;
import br.com.hyteck.school_control.exceptions.ResourceNotFoundException;
import br.com.hyteck.school_control.models.classrooms.ClassRoom;
import br.com.hyteck.school_control.repositories.ClassroomRepository;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.Objects;

@Service
@Validated
public class UpdateClassRoom {

    private static final Logger logger = LoggerFactory.getLogger(UpdateClassRoom.class);
    private final ClassroomRepository classRoomRepository;

    public UpdateClassRoom(ClassroomRepository classRoomRepository) {
        this.classRoomRepository = classRoomRepository;
    }

    @Transactional
    public ClassRoomResponse execute(String id, @Valid ClassRoomRequest requestDTO) {
        logger.info("Iniciando atualização da ClassRoom com ID: {}", id);

        // 1. Buscar existente
        ClassRoom existingClassRoom = classRoomRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("ClassRoom não encontrada para atualização. ID: {}", id);
                    return new ResourceNotFoundException("Turma não encontrada com ID: " + id);
                });

        // 2. Verificar duplicidade SE nome ou ano mudaram
        checkDuplicates(requestDTO, existingClassRoom);

        // 3. Atualizar entidade
        updateEntityFromDto(existingClassRoom, requestDTO);

        // 4. Salvar
        ClassRoom updatedClassRoom = classRoomRepository.save(existingClassRoom);
        logger.info("ClassRoom atualizada com sucesso. ID: {}", updatedClassRoom.getId());

        // 5. Retornar DTO
        return ClassRoomResponse.from(updatedClassRoom);
    }

    private void checkDuplicates(ClassRoomRequest requestDTO, ClassRoom existingClassRoom) {
        // Verifica apenas se nome ou ano foram alterados
        if (!Objects.equals(requestDTO.name(), existingClassRoom.getName()) ||
                !Objects.equals(requestDTO.schoolYear(), existingClassRoom.getYear())) {

            classRoomRepository.findByNameAndYear(requestDTO.name(), requestDTO.schoolYear())
                    .ifPresent(duplicate -> {
                        // Permite se o duplicado for o próprio registro sendo atualizado
                        if (!Objects.equals(duplicate.getId(), existingClassRoom.getId())) {
                            String message = "Já existe outra turma com o nome '" + requestDTO.name() + "' para o ano letivo " + requestDTO.schoolYear();
                            logger.warn("Tentativa de atualizar ClassRoom ID {} com dados duplicados: {}", existingClassRoom.getId(), message);
                            throw new DuplicateResourceException(message);
                        }
                    });
        }
    }

    private void updateEntityFromDto(ClassRoom entity, ClassRoomRequest dto) {
        entity.setName(dto.name());
        entity.setYear(dto.schoolYear());
        // Atualizar outros campos
    }
}