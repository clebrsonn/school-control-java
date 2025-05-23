package br.com.hyteck.school_control.models.payments;

import br.com.hyteck.school_control.models.auth.User;
import br.com.hyteck.school_control.models.classrooms.Student;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Entity
@Table(name = "responsibles")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class Responsible extends User {

    @NotBlank
    @Size(min=2)
    private String name;

    @NotBlank
    private String phone;

    @OneToMany(mappedBy = "responsible",  cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Student> students;

    public Responsible(String id, String name, String email, String phone) {
        this.setId(id);
        super.setEmail(email);
        this.name = name;

        this.phone = phone;
    }
}
