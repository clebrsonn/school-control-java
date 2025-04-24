package br.com.hyteck.school_control.repositories;

import br.com.hyteck.school_control.models.auth.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Set;

public interface RoleRepository extends JpaRepository<Role, String> {
    Set<Role> findByNameIn(Set<String> names);

    boolean existsByName(String name);
}