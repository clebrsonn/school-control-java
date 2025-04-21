package br.com.hyteck.school_control.usecases.classroom;

import br.com.hyteck.school_control.web.dtos.classroom.ClassRoomResponse;
import br.com.hyteck.school_control.repositories.ClassroomRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class FindClassRoomById {

    private static final Logger logger = LoggerFactory.getLogger(FindClassRoomById.class);
    private final ClassroomRepository classRoomRepository;

    public FindClassRoomById(ClassroomRepository classRoomRepository) {
        this.classRoomRepository = classRoomRepository;
    }

    @Transactional(readOnly = true)
    public Optional<ClassRoomResponse> execute(String id) {
        logger.debug("Buscando ClassRoom com ID: {}", id);
        return classRoomRepository.findById(id)
                .map(ClassRoomResponse::from);
    }
}