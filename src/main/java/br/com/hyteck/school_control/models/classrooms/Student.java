package br.com.hyteck.school_control.models.classrooms;

import br.com.hyteck.school_control.models.AbstractModel;
import br.com.hyteck.school_control.models.payments.Responsible;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.validator.constraints.br.CPF;

import java.util.ArrayList;
import java.util.List;

@SuperBuilder
@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "students")
public class Student extends AbstractModel {

    @NotBlank
    private String name;
    @Email
    private String email;

    @CPF
    private String cpf;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    @JoinColumn(name = "responsible_id", nullable = false)
    private Responsible responsible;

    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Enrollment> enrollments = new ArrayList<>();
}
