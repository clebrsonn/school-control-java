package br.com.hyteck.school_control.repositories;

import br.com.hyteck.school_control.models.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByUsername(String userName);

}