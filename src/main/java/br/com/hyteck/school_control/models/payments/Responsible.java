package br.com.hyteck.school_control.models.payments;

import br.com.hyteck.school_control.models.AbstractModel;
import br.com.hyteck.school_control.models.classrooms.Student;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Entity
@Table
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class Responsible extends AbstractModel {

    @NotBlank
    @Size(min=2)
    private String name;

    @Email
    private String email;

    @NotBlank
    private String phone;

    @OneToMany(mappedBy = "responsible",  cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
    private List<Student> students;
}
