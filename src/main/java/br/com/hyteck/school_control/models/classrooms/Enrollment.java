package br.com.hyteck.school_control.models.classrooms;

import br.com.hyteck.school_control.models.AbstractModel;
import br.com.hyteck.school_control.models.payments.Invoice; // Importar Invoice
import jakarta.persistence.*;
import lombok.*; // Usar Lombok

import java.time.LocalDateTime;
import java.util.ArrayList; // Inicializar a lista
import java.util.List; // Usar List

@Entity
@Table
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder // Adicionar Builder
public class Enrollment extends AbstractModel {

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST) // Ajustar cascade conforme necessidade
    @JoinColumn(name = "classroom_id", nullable = false) // Adicionar JoinColumn
    private ClassRoom classroom;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST) // Ajustar cascade
    @JoinColumn(name = "student_id", nullable = false) // Adicionar JoinColumn
    private Student student;

    private Boolean isActive;

    private LocalDateTime startDate;
    private LocalDateTime endDate;

    // Uma matrícula pode ter várias faturas ao longo do tempo
    @OneToMany(mappedBy = "enrollment", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default // Inicializar com Builder
    private List<Invoice> invoices = new ArrayList<>(); // Inicializar a lista

}