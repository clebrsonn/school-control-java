package br.com.hyteck.school_control.models.auth;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a verification token typically used for processes such as email account verification.
 * Each token is associated with a {@link User}, has a unique value, and an expiration date.
 * Lombok's annotations ({@link Getter}, {@link Setter}, {@link NoArgsConstructor}, {@link AllArgsConstructor})
 * are used to generate common boilerplate code.
 */
@Entity
@Table(name = "verification_tokens") // Specifies the database table name.
@Getter // Lombok: Generates getter methods.
@Setter // Lombok: Generates setter methods.
@NoArgsConstructor // Lombok: Generates a no-argument constructor, useful for JPA.
@AllArgsConstructor // Lombok: Generates a constructor with all arguments.
public class VerificationToken {

    /**
     * Defines the duration in hours for which the token remains valid after creation.
     * Currently set to 24 hours.
     */
    private static final int EXPIRATION_HOURS = 24;

    /**
     * The unique identifier for the verification token entity.
     * Generated automatically as a UUID string.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID) // Specifies that the ID is a generated UUID.
    private String id;

    /**
     * The actual token string, which is a randomly generated UUID.
     * This token is sent to the user (e.g., via email) to verify their account.
     * It must be unique and not null.
     */
    @Column(nullable = false, unique = true)
    private String token;

    /**
     * The user associated with this verification token.
     * This is a one-to-one relationship, fetched eagerly. The {@code user_id} column in the
     * {@code verification_tokens} table serves as the foreign key to the {@code users} table.
     */
    @OneToOne(targetEntity = User.class, fetch = FetchType.EAGER) // Defines the one-to-one relationship.
    @JoinColumn(nullable = false, name = "user_id", referencedColumnName = "id") // Specifies the foreign key.
    private User user;

    /**
     * The date and time when this verification token expires.
     * It is calculated based on the {@link #EXPIRATION_HOURS} constant from the moment of creation.
     * Must not be null.
     */
    @Column(nullable = false)
    private LocalDateTime expiryDate;

    /**
     * Constructs a new VerificationToken for a given user.
     * Automatically generates a unique token string and calculates the expiry date.
     *
     * @param user The {@link User} for whom this verification token is being created. Must not be null.
     */
    public VerificationToken(User user) {
        this.user = user;
        this.token = UUID.randomUUID().toString(); // Generate a random UUID for the token.
        this.expiryDate = calculateExpiryDate(); // Calculate the token's expiry date.
    }

    /**
     * Calculates the expiry date for the token based on the current time and the predefined {@link #EXPIRATION_HOURS}.
     *
     * @return The {@link LocalDateTime} representing the exact date and time of expiration.
     */
    private LocalDateTime calculateExpiryDate() {
        return LocalDateTime.now().plusHours(EXPIRATION_HOURS);
    }

    /**
     * Checks if the verification token has expired by comparing the current date and time
     * with the token's {@link #expiryDate}.
     *
     * @return {@code true} if the token has expired, {@code false} otherwise.
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiryDate);
    }
}