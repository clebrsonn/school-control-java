package br.com.hyteck.school_control.models.auth;

import br.com.hyteck.school_control.listeners.UserEntityListener;
import br.com.hyteck.school_control.models.AbstractModel;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Represents a user in the system.
 * This entity stores user credentials (username, password), contact information (email),
 * assigned roles for authorization, and account status flags.
 * It implements Spring Security's {@link UserDetails} interface to integrate with the
 * authentication and authorization mechanisms.
 * Extends {@link AbstractModel} for common ID and timestamp fields.
 * Lombok's annotations are used for boilerplate code generation.
 * The {@link EntityListeners} annotation links it to {@link UserEntityListener} for lifecycle events.
 */
@EntityListeners(value= UserEntityListener.class)
@Entity
@Table(name = "users") // Specifies the database table name.
@Getter // Lombok: Generates getter methods.
@Setter // Lombok: Generates setter methods.
@NoArgsConstructor // Lombok: Generates a no-argument constructor.
@AllArgsConstructor // Lombok: Generates a constructor with all arguments.
@SuperBuilder // Lombok: Enables builder pattern, including fields from AbstractModel.
@Inheritance(strategy = InheritanceType.JOINED) // Defines table-per-subclass inheritance strategy.
public class User extends AbstractModel implements UserDetails {

    /**
     * The username of the user, used for login. Must be unique and not null.
     */
    @Column(unique = true, nullable = false)
    private String username;

    /**
     * The hashed password for the user. Must not be null.
     * Raw passwords should never be stored; this field holds the result of a hashing algorithm.
     */
    @Column(nullable = false)
    private String password;

    /**
     * The email address of the user. Must be unique and not null.
     * Used for communication and potentially for account recovery or notifications.
     */
    @Column(unique = true, nullable = false)
    private String email;

    /**
     * A set of {@link Role} objects assigned to this user.
     * Determines the user's permissions within the application.
     * Fetched eagerly to ensure roles are available when the user is loaded.
     * The relationship is managed via a join table named "user_roles".
     */
    @ManyToMany(fetch = FetchType.EAGER) // Roles are fetched eagerly with the User.
    @JoinTable(
            name = "user_roles", // Name of the intermediate table.
            joinColumns = @JoinColumn(name = "user_id"), // Foreign key in join table linking to User.
            inverseJoinColumns = @JoinColumn(name = "role") // Foreign key in join table linking to Role.
    )
    @Builder.Default // Ensures roles is initialized to an empty HashSet by Lombok's builder.
    private Set<Role> roles = new HashSet<>();

    /**
     * Indicates whether the user's account has expired. An expired account cannot be authenticated.
     * Defaults to {@code true} (not expired).
     */
    @Builder.Default
    private boolean accountNonExpired = true;

    /**
     * Indicates whether the user is locked or unlocked. A locked account cannot be authenticated.
     * Defaults to {@code true} (not locked).
     */
    @Builder.Default
    private boolean accountNonLocked = true;

    /**
     * Indicates whether the user's credentials (password) has expired. Expired credentials prevent authentication.
     * Defaults to {@code true} (not expired).
     */
    @Builder.Default
    private boolean credentialsNonExpired = true;

    /**
     * Indicates whether the user is enabled or disabled. A disabled user cannot be authenticated.
     * Defaults to {@code false} (disabled), typically requiring email verification to enable.
     */
    @Builder.Default
    private boolean enabled = false;

    /**
     * The verification token associated with this user, used for processes like email verification.
     * This is a one-to-one relationship, and operations on User may cascade to VerificationToken.
     * `orphanRemoval = true` means the VerificationToken is deleted if it's removed from this relationship.
     */
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private VerificationToken verificationToken;

    /**
     * Returns the authorities granted to the user.
     * This implementation maps the user's {@link Role} objects to a collection of {@link SimpleGrantedAuthority}.
     *
     * @return A collection of granted authorities.
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Converts the Set<Role> to a List<SimpleGrantedAuthority>.
        return this.roles.stream()
                .map(role -> new SimpleGrantedAuthority(role.getName()))
                .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isAccountNonExpired() {
        return this.accountNonExpired;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isAccountNonLocked() {
        return this.accountNonLocked;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return this.credentialsNonExpired;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEnabled() {
        return this.enabled;
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     * Equality is based solely on the {@code username}.
     *
     * @param o The reference object with which to compare.
     * @return {@code true} if this object is the same as the obj argument; {@code false} otherwise.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        // Check if 'o' is an instance of User, using getClass() for exact type matching.
        if (o == null || getClass() != o.getClass()) return false;

        User user = (User) o;

        // Users are considered equal if their usernames are equal.
        return Objects.equals(username, user.username);
    }

    /**
     * Returns a hash code value for the object.
     * The hash code is based on the {@code id} if present, otherwise defaults to 0.
     * This ensures consistency with the {@code equals} method if IDs are unique and stable after being set.
     * However, relying on a mutable ID for hashCode, especially before it's persisted and assigned,
     * can be problematic if instances are put into HashMaps/HashSets before ID generation.
     * A common alternative is to use a business key like 'username' if it's immutable after creation.
     *
     * @return A hash code value for this object.
     */
    @Override
    public int hashCode() {
        // Using 'id' for hashCode. Ensure 'id' is set and immutable for consistent hashing in collections.
        // If 'id' can be null (e.g., before persistence), this might lead to all unsaved entities having hash 0.
        // Consider using Objects.hash(username) if username is a stable unique identifier.
        return id != null ? id.hashCode() : 0;
    }

}