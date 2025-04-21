package br.com.hyteck.school_control.usecases.classroom;

import br.com.hyteck.school_control.exceptions.DeletionNotAllowedException;
import br.com.hyteck.school_control.exceptions.ResourceNotFoundException;

import br.com.hyteck.school_control.models.classrooms.ClassRoom;
import br.com.hyteck.school_control.repositories.ClassroomRepository;
import br.com.hyteck.school_control.repositories.EnrollmentRepository; // Assumindo que existe
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeleteClassRoom {

    private static final Logger logger = LoggerFactory.getLogger(DeleteClassRoom.class);
    private final ClassroomRepository classRoomRepository;
    private final EnrollmentRepository enrollmentRepository; // Injete para verificar matrículas

    public DeleteClassRoom(ClassroomRepository classRoomRepository, EnrollmentRepository enrollmentRepository) {
        this.classRoomRepository = classRoomRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    @Transactional
    public void execute(String id) {
        logger.info("Iniciando exclusão da ClassRoom com ID: {}", id);

        // 1. Verificar existência
        ClassRoom classRoomToDelete = classRoomRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("ClassRoom não encontrada para exclusão. ID: {}", id);
                    return new ResourceNotFoundException("Turma não encontrada com ID: " + id);
                });
        try {
            classRoomToDelete.validateDeletionPrerequisites(enrollmentRepository);
        }catch (DeletionNotAllowedException e){
            logger.warn("Tentativa de excluir ClassRoom ID {} violou pré-requisitos: {}", id, e.getMessage());
            throw e; // Relança a exceção para ser tratada pelo ControllerAdvice
        }

        classRoomRepository.deleteById(id);
        logger.info("ClassRoom excluída com sucesso. ID: {}", id);
    }
}