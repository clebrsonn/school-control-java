package br.com.hyteck.school_control.usecases.enrollment; // Ou usecases.classroom

import br.com.hyteck.school_control.models.classrooms.Enrollment;
import br.com.hyteck.school_control.repositories.EnrollmentRepository;
import br.com.hyteck.school_control.web.dtos.classroom.EnrollmentResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class FindEnrollmentsByStudentId {

    private static final Logger logger = LoggerFactory.getLogger(FindEnrollmentsByStudentId.class);
    private final EnrollmentRepository enrollmentRepository;

    public FindEnrollmentsByStudentId(EnrollmentRepository enrollmentRepository) {
        this.enrollmentRepository = enrollmentRepository;
    }

    @Transactional(readOnly = true) // Otimização para leitura
    public List<EnrollmentResponse> execute(String studentId) {  // studentId como String, ajuste se necessário
        List<Enrollment> enrollments = enrollmentRepository.findByStudentId(studentId); // Substitua pela sua lógica de busca
        return enrollments.stream()
                .map(EnrollmentResponse::from)  // Precisamos do método from() no DTO
                .collect(Collectors.toList());
    }
}