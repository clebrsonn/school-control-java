package br.com.hyteck.school_control.models.classrooms;

import br.com.hyteck.school_control.models.AbstractModel;
import br.com.hyteck.school_control.models.payments.Responsible;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.validator.constraints.br.CPF;

import java.util.ArrayList;
import java.util.List;

@Builder
@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table
public class Student extends AbstractModel {

    @NotBlank
    private String name;
    @Email
    private String email;

    @CPF
    private String cpf;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST) // Cuidado com CascadeType.ALL aqui
    @JoinColumn(name = "responsible_id", nullable = false) // FK para Responsible
    private Responsible responsible;

    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Enrollment> enrollments = new ArrayList<>();
}
