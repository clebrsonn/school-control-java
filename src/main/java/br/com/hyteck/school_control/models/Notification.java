package br.com.hyteck.school_control.models;

import br.com.hyteck.school_control.models.auth.User;
import br.com.hyteck.school_control.models.payments.Responsible;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Notification extends AbstractModel{

    @ManyToOne(cascade = CascadeType.ALL)
    private User user;

    private String message;

    private LocalDateTime sendAt;

    @Column
    private LocalDateTime readAt;

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private boolean read = false;
}
