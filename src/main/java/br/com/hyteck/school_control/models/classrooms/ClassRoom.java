package br.com.hyteck.school_control.models.classrooms;

import br.com.hyteck.school_control.exceptions.DeletionNotAllowedException;
import br.com.hyteck.school_control.models.AbstractModel;
import br.com.hyteck.school_control.repositories.EnrollmentRepository;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ClassRoom extends AbstractModel {
    private String name;
    @OneToMany(mappedBy = "classroom", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Enrollment> enrollments = new ArrayList<>();

    private String year;

    // --- NOVO MÉTODO DE COMPORTAMENTO ---
    /**
     * Verifica se a turma pode ser excluída, lançando uma exceção se houver
     * matrículas associadas.
     *
     * @param enrollmentRepository O repositório para verificar a existência de matrículas.
     * @throws DeletionNotAllowedException Se existirem matrículas associadas.
     */
    public void validateDeletionPrerequisites(EnrollmentRepository enrollmentRepository) {
        if (enrollmentRepository.existsByClassroomId(this.getId())) { // Usa o ID da própria instância
            String message = "Não é possível excluir a turma '" + this.name + "' (" + this.getYear() + ") pois existem matrículas associadas.";
            // O logger aqui dentro é debatível, geralmente fica no serviço/use case
            throw new DeletionNotAllowedException(message);
        }
        // Poderia adicionar outras validações de pré-requisito aqui
    }

}
