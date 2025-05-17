package br.com.hyteck.school_control.usecases.classroom;

import br.com.hyteck.school_control.repositories.ClassroomRepository;
import br.com.hyteck.school_control.web.dtos.classroom.ClassRoomResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Log4j2
public class FindClassRoomById {

    private final ClassroomRepository classRoomRepository;

    @Transactional(readOnly = true)
    public Optional<ClassRoomResponse> execute(String id) {
        log.debug("Buscando ClassRoom com ID: {}", id);
        return classRoomRepository.findById(id)
                .map(ClassRoomResponse::from);
    }
}