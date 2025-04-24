package br.com.hyteck.school_control.usecases.enrollment; // Ou usecases.classroom

import br.com.hyteck.school_control.models.classrooms.Enrollment;
import br.com.hyteck.school_control.repositories.EnrollmentRepository;
import br.com.hyteck.school_control.web.dtos.classroom.EnrollmentResponse; // Use o DTO de resposta correto
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FindEnrollmentsByClassRoomId {

    private static final Logger logger = LoggerFactory.getLogger(FindEnrollmentsByClassRoomId.class);
    private final EnrollmentRepository enrollmentRepository;

    public FindEnrollmentsByClassRoomId(EnrollmentRepository enrollmentRepository) {
        this.enrollmentRepository = enrollmentRepository;
    }

    @Transactional(readOnly = true) // Otimização para leitura
    public Page<EnrollmentResponse> execute(String classroomId, Pageable pageable) {
        logger.info("Buscando matrículas para a turma ID: {} com paginação: {}", classroomId, pageable);

        // Busca as matrículas no repositório
        Page<Enrollment> enrollmentPage = enrollmentRepository.findByClassroomId(classroomId, pageable);

        // Mapeia a página de entidades para uma página de DTOs de resposta
        Page<EnrollmentResponse> responsePage = enrollmentPage.map(EnrollmentResponse::from); // Usa o método estático 'from' do DTO

        logger.debug("Encontradas {} matrículas na página atual para a turma ID: {}", responsePage.getNumberOfElements(), classroomId);
        return responsePage;
    }
}