package br.com.hyteck.school_control.models;

import br.com.hyteck.school_control.models.payments.Responsible;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table
public class Notification{
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(cascade = CascadeType.ALL)
    private Responsible responsible;

    private String message;

    private LocalDateTime sendAt;
}
