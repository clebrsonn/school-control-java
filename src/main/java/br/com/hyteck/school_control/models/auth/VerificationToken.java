package br.com.hyteck.school_control.models.auth;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "verification_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VerificationToken {

    private static final int EXPIRATION_HOURS = 24;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true)
    private String token; // O UUID real

    @OneToOne(targetEntity = User.class, fetch = FetchType.EAGER)
    @JoinColumn(nullable = false, name = "user_id", referencedColumnName = "id") // FK para User
    private User user;

    @Column(nullable = false)
    private LocalDateTime expiryDate;

    public VerificationToken(User user) {
        this.user = user;
        this.token = UUID.randomUUID().toString();
        this.expiryDate = calculateExpiryDate();
    }

    private LocalDateTime calculateExpiryDate() {
        return LocalDateTime.now().plusHours(EXPIRATION_HOURS);
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiryDate);
    }
}