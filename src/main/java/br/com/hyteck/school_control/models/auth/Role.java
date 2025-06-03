package br.com.hyteck.school_control.models.auth;

import jakarta.persistence.*;
import lombok.*;

/**
 * Represents a user role within the application's security context.
 * Roles are used to define permissions and access control for users.
 * Common examples include "ROLE_USER", "ROLE_ADMIN", "ROLE_TEACHER".
 * The role name itself acts as the primary key.
 * Lombok's annotations ({@link Getter}, {@link Setter}, {@link NoArgsConstructor}, {@link AllArgsConstructor}, {@link Builder})
 * are used to generate common boilerplate code.
 */
@Entity
@Table(name = "roles") // Specifies the database table name.
@Getter // Lombok: Generates getter methods for all fields.
@Setter // Lombok: Generates setter methods for all fields.
@NoArgsConstructor // Lombok: Generates a no-argument constructor.
@AllArgsConstructor // Lombok: Generates a constructor with all arguments.
@Builder // Lombok: Provides the builder pattern for object creation.
public class Role {

    /**
     * The name of the role, serving as its unique identifier (primary key).
     * For example, "ROLE_ADMIN", "ROLE_USER".
     * The length is constrained to 50 characters.
     * This field should not be null and is typically managed as a predefined set of roles in the system.
     */
    @Id // Marks this field as the primary key.
    @Column(length = 50) // Specifies column constraints, such as length.
    private String name;

}
