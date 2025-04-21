package br.com.hyteck.school_control.repositories;

import br.com.hyteck.school_control.models.Role;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, String> {
}