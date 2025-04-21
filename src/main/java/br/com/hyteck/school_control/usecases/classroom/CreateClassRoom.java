package br.com.hyteck.school_control.usecases.classroom;

import br.com.hyteck.school_control.web.dtos.classroom.ClassRoomRequest;
import br.com.hyteck.school_control.web.dtos.classroom.ClassRoomResponse;
import br.com.hyteck.school_control.exceptions.DuplicateResourceException;
import br.com.hyteck.school_control.models.classrooms.ClassRoom;
import br.com.hyteck.school_control.repositories.ClassroomRepository;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
public class CreateClassRoom {

    private static final Logger logger = LoggerFactory.getLogger(CreateClassRoom.class);
    private final ClassroomRepository classRoomRepository;

    public CreateClassRoom(ClassroomRepository classRoomRepository) {
        this.classRoomRepository = classRoomRepository;
    }

    @Transactional
    public ClassRoomResponse execute(@Valid ClassRoomRequest requestDTO) {
        logger.info("Iniciando criação de ClassRoom: {} ({})", requestDTO.name(), requestDTO.schoolYear());

        // 1. Verificar duplicidade
        if (classRoomRepository.existsByNameAndYear(requestDTO.name(), requestDTO.schoolYear())) {
            String message = "Já existe uma turma com o nome '" + requestDTO.name() + "' para o ano letivo " + requestDTO.schoolYear();
            logger.warn(message);
            throw new DuplicateResourceException(message);
        }

        // 2. Mapear DTO para Entidade
        ClassRoom classRoomToSave = ClassRoomRequest.to(requestDTO);

        // 3. Persistir
        ClassRoom savedClassRoom = classRoomRepository.save(classRoomToSave);
        logger.info("ClassRoom criada com sucesso. ID: {}", savedClassRoom.getId());

        // 4. Mapear para Resposta
        return ClassRoomResponse.from(savedClassRoom);
    }


}