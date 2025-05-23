package br.com.hyteck.school_control.repositories;

import br.com.hyteck.school_control.models.auth.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for {@link User} entities.
 * Provides CRUD operations and custom query methods for accessing user data.
 */
@Repository
public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByUsername(String username);

    /**
     * Checks if a user exists with the given email address.
     *
     * @param email The email address to check.
     * @return {@code true} if a user with the specified email exists, {@code false} otherwise.
     */
    boolean existsByEmail(String email);

    /**
     * Finds a user by their username.
     *
     * @param username The username to search for.
     * @return An {@link Optional} containing the {@link User} if found, or an empty Optional if not.
     */

    @Query("SELECT u FROM User u JOIN FETCH u.roles WHERE u.username = :username")
    Optional<User> findByUsername(String username);


}
