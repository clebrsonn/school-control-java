package br.com.hyteck.school_control.models;

import br.com.hyteck.school_control.models.auth.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Notification extends AbstractModel{

    @ManyToOne(cascade = CascadeType.ALL)
    private User user;

    private String message;

    private String link;

    private String type;

    private LocalDateTime sendAt;

    @Column
    private LocalDateTime readAt;

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private boolean read = false;
}
