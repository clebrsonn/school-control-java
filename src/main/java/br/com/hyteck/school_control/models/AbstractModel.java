package br.com.hyteck.school_control.models;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;

/**
 * An abstract base class for entity models, providing common fields such as a unique identifier (`id`),
 * creation timestamp (`createdAt`), and update timestamp (`updatedAt`).
 * This class is intended to be extended by other entity classes.
 *
 * The {@link MappedSuperclass} annotation indicates that this class's mapping information is applied
 * to the entities that inherit from it.
 * Lombok's {@link Getter}, {@link Setter}, {@link SuperBuilder}, {@link NoArgsConstructor},
 * and {@link AllArgsConstructor} annotations are used to generate common boilerplate code.
 */
@Getter
@Setter
@MappedSuperclass
@SuperBuilder // Enables the builder pattern for this class and its subclasses.
@NoArgsConstructor // Generates a no-argument constructor.
@AllArgsConstructor // Generates an all-argument constructor.
public abstract class AbstractModel {

    /**
     * The unique identifier for the entity.
     * It is generated automatically as a UUID string.
     * This field is protected to allow access from subclasses if needed, but direct modification
     * is generally handled by JPA.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID) // Specifies that the ID is a generated UUID.
    protected String id;

    /**
     * Timestamp indicating when the entity was first persisted.
     * Automatically set upon creation by {@link CreationTimestamp} (Hibernate) and {@link CreatedDate} (JPA auditing).
     * This field is not updatable after initial creation.
     */
    @CreationTimestamp // Hibernate annotation to set timestamp on creation.
    @CreatedDate       // JPA auditing annotation (if JPA auditing is enabled).
    @Column(nullable = false, updatable = false) // Ensures the column is not null and cannot be updated.
    private LocalDateTime createdAt;

    /**
     * Timestamp indicating when the entity was last updated.
     * Automatically set/updated on modification by {@link UpdateTimestamp} (Hibernate) and {@link LastModifiedDate} (JPA auditing).
     * This field is not nullable.
     */
    @UpdateTimestamp   // Hibernate annotation to set/update timestamp on modification.
    @LastModifiedDate  // JPA auditing annotation (if JPA auditing is enabled).
    @Column(nullable = false) // Ensures the column is not null.
    private LocalDateTime updatedAt;

}
