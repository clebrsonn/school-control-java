package br.com.hyteck.school_control.usecases.classroom;

import br.com.hyteck.school_control.web.dtos.classroom.ClassRoomResponse;
import br.com.hyteck.school_control.repositories.ClassroomRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FindClassRooms {

    private static final Logger logger = LoggerFactory.getLogger(FindClassRooms.class);
    private final ClassroomRepository classRoomRepository;

    public FindClassRooms(ClassroomRepository classRoomRepository) {
        this.classRoomRepository = classRoomRepository;
    }

    @Transactional(readOnly = true)
    public Page<ClassRoomResponse> execute(Pageable pageable) {
        logger.info("Buscando todas as ClassRooms paginadas: {}", pageable);
        return classRoomRepository.findAll(pageable)
                .map(ClassRoomResponse::from);
    }
}