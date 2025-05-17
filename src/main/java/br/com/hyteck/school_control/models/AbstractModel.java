package br.com.hyteck.school_control.models;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;

@Getter
@Setter
@MappedSuperclass
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class AbstractModel {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID) // Using UUID like in Expense entity
    protected String id;

    @CreationTimestamp
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

}
