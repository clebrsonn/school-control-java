package br.com.hyteck.school_control.repositories;

import br.com.hyteck.school_control.models.classrooms.Enrollment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

public interface EnrollmentRepository extends JpaRepository<Enrollment, String> { // Assume ID String
    List<Enrollment> findByStudentId(String studentId);

    @EntityGraph(attributePaths = {
            "classroom",
            "student",
            "invoiceItems"
    })
    List<Enrollment> findByStudentIdAndStatus(String studentId, Enrollment.Status status);
    /**
     * Verifica se já existe uma matrícula para um estudante específico em uma turma específica.
     *
     * @param studentId   ID do estudante.
     * @param classRoomId ID da turma.
     * @return true se a matrícula já existe, false caso contrário.
     */
    boolean existsByStudentIdAndClassroomId(String studentId, String classRoomId);

    /**
     * Verifica se existem matrículas associadas a uma determinada turma.
     * (Este método já existe no seu ClassRoom.java, mas pode ser útil tê-lo aqui também)
     *
     * @param classroomId ID da turma.
     * @return true se existem matrículas, false caso contrário.
     */
    boolean existsByClassroomId(String classroomId);

    /**
     * (Opcional) Verifica se um estudante já está matriculado em alguma turma no mesmo ano.
     *
     * @param studentId ID do estudante.
     * @param year      Ano letivo.
     * @return true se já existe matrícula naquele ano, false caso contrário.
     */
    boolean existsByStudentIdAndClassroomYear(String studentId, String year);

    /**
     * (Opcional) Verifica se um estudante já está matriculado em alguma turma no mesmo ano.
     *
     * @param studentId ID do estudante.
     * @param year      Ano letivo.
     * @return true se já existe matrícula naquele ano, false caso contrário.
     */
    Optional<Enrollment> findByStudentIdAndClassroomYearAndStatus(String studentId, String year, Enrollment.Status status);


    /**
     * Busca todas as matrículas associadas a uma turma específica, com paginação.
     *
     * @param classroomId O ID da ClassRoom.
     * @param pageable    Informações de paginação e ordenação.
     * @return Uma página de Enrollments para a turma especificada.
     */
    @EntityGraph(attributePaths = {
            "classroom",
            "student",
            "invoiceItems"
    })
    Page<Enrollment> findByClassroomId(String classroomId, Pageable pageable);

    @EntityGraph(attributePaths = {
            "classroom",
            "student",
            "invoiceItems"
    })
    List<Enrollment> findByStatus(Enrollment.Status status);
}
