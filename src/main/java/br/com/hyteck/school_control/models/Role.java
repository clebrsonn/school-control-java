package br.com.hyteck.school_control.models;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role {
    @Id
    @Column(length = 50) // Role names are usually shorter
    private String name; // e.g., "ROLE_USER", "ROLE_ADMIN"

}
