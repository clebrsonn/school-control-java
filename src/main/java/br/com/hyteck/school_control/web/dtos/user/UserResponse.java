package br.com.hyteck.school_control.web.dtos.user;

import br.com.hyteck.school_control.models.auth.Role;
import br.com.hyteck.school_control.models.auth.User; // Importar entidade User

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * DTO para retornar dados de um usuário. NÃO inclui a senha.
 */
public record UserResponse(
        String id,
        String username,
        String email,
        Set<String> roles,
        boolean enabled,
        boolean accountNonLocked,
        boolean accountNonExpired,
        boolean credentialsNonExpired,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static UserResponse from(User user) {
        if (user == null) {
            return null;
        }
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRoles().stream().map(Role::getName).collect(Collectors.toSet()), // Mapeia roles para nomes
                user.isEnabled(),
                user.isAccountNonLocked(),
                user.isAccountNonExpired(),
                user.isCredentialsNonExpired(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}