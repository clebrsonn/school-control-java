package br.com.hyteck.school_control.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;

@Getter
@Setter
@MappedSuperclass
public abstract class AbstractModel {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO) // Using UUID like in Expense entity
    protected String id;

    @CreationTimestamp
    @CreatedDate
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @LastModifiedDate
    private LocalDateTime updatedAt;

}
