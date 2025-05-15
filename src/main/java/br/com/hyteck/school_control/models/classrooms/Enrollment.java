package br.com.hyteck.school_control.models.classrooms;

import br.com.hyteck.school_control.exceptions.DuplicateResourceException;
import br.com.hyteck.school_control.models.AbstractModel;
import br.com.hyteck.school_control.models.payments.Invoice; // Importar Invoice
import br.com.hyteck.school_control.models.payments.InvoiceItem;
import br.com.hyteck.school_control.repositories.EnrollmentRepository;
import jakarta.persistence.*;
import lombok.*; // Usar Lombok
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList; // Inicializar a lista
import java.util.List; // Usar List

@Entity
@Table(name = "enrollments")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class Enrollment extends AbstractModel {
    public enum Status {
        ACTIVE,
        PENDING,
        CANCELLED,
        TRANSFERRED
    }

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Status status;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST) // Ajustar cascade conforme necessidade
    @JoinColumn(name = "classroom_id", nullable = false) // Adicionar JoinColumn
    private ClassRoom classroom;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST) // Ajustar cascade
    @JoinColumn(name = "student_id", nullable = false) // Adicionar JoinColumn
    private Student student;

    private LocalDateTime startDate;
    private LocalDateTime endDate;

    private BigDecimal enrollmentFee;
    private BigDecimal monthlyFee;

    @OneToMany(mappedBy = "enrollment", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<InvoiceItem> invoiceItems = new ArrayList<>(); // Inicializar a lista

    @PrePersist
    protected void onCreate() {
        if (startDate == null) {
            startDate = LocalDateTime.now();
        }
    }

    public void validateEnrollmentRules(EnrollmentRepository enrollmentRepository) {
        // a) Verificar se já existe matrícula do estudante NESSA turma
        if (enrollmentRepository.existsByStudentIdAndClassroomId(student.getId(), classroom.getId())) {
            throw new DuplicateResourceException(
                    "Estudante '" + student.getName() + "' já está matriculado na turma '" +
                            classroom.getName() + " (" + classroom.getYear() + ")'."
            );
        }

        // b) (Opcional) Verificar se o estudante já está matriculado em ALGUMA turma NESTE ano
        if (enrollmentRepository.existsByStudentIdAndClassroomYear(student.getId(), classroom.getYear())) {
            throw new RuntimeException(
                    "Estudante '" + student.getName() + "' já possui matrícula em outra turma para o ano " + classroom.getYear() + "."
            );
        }

        // c) (Opcional) Verificar limite de vagas na turma?
        // int maxCapacity = 30; // Exemplo, poderia vir da turma ou configuração
        // int currentEnrollments = enrollmentRepository.countByClassroomId(classRoom.getId()); // Método a criar
        // if (currentEnrollments >= maxCapacity) {
        //     throw new BusinessRuleException("A turma '" + classRoom.getName() + "' atingiu a capacidade máxima.");
        // }

        // Outras regras? (Ex: pré-requisitos, status do estudante, etc.)
    }

}
