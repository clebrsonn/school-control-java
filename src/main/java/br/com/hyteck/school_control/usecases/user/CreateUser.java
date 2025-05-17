package br.com.hyteck.school_control.usecases.user;

import br.com.hyteck.school_control.exceptions.DuplicateResourceException;
import br.com.hyteck.school_control.exceptions.ResourceNotFoundException;
import br.com.hyteck.school_control.models.auth.Role;
import br.com.hyteck.school_control.models.auth.User;
import br.com.hyteck.school_control.repositories.RoleRepository;
import br.com.hyteck.school_control.repositories.UserRepository;
import br.com.hyteck.school_control.web.dtos.user.UserRequest;
import br.com.hyteck.school_control.web.dtos.user.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@Validated
@Log4j2
@RequiredArgsConstructor
public class CreateUser {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    private final CreateVerificationToken createVerification;

    @Transactional
    public UserResponse execute(@Valid UserRequest requestDTO) {
        log.info("Iniciando criação de usuário: {}", requestDTO.username());

        if (!StringUtils.hasText(requestDTO.password())) {
            throw new IllegalArgumentException("Senha é obrigatória para criar usuário."); // Ou BusinessRuleException
        }
        if (userRepository.findByUsername(requestDTO.username()).isPresent()) {
            throw new DuplicateResourceException("Username já cadastrado: " + requestDTO.username());
        }
        if (userRepository.existsByEmail(requestDTO.email())) {
            log.warn("Tentativa de criar com email duplicado: {}", requestDTO.email());
            throw new DuplicateResourceException("Email já cadastrado: " + requestDTO.email());
        }

        Set<Role> roles = findAndValidateRoles(Set.of("ROLE_USER"));

        User userToSave = User.builder()
                .username(requestDTO.username())
                .password(requestDTO.password())
                .email(requestDTO.email())
                .roles(roles)
                .build();

        User savedUser = userRepository.save(userToSave);
        log.info("Usuário criado com sucesso. ID: {}", savedUser.getId());
        createVerification.execute(savedUser);
        return UserResponse.from(savedUser);
    }

    private Set<Role> findAndValidateRoles(Set<String> roleNames) {
        Set<Role> foundRoles = roleRepository.findByNameIn(roleNames);
        if (foundRoles.size() != roleNames.size()) {
            Set<String> foundRoleNames = foundRoles.stream().map(Role::getName).collect(Collectors.toSet());
            Set<String> missingRoles = roleNames.stream()
                    .filter(name -> !foundRoleNames.contains(name))
                    .collect(Collectors.toSet());
            throw new ResourceNotFoundException("Roles não encontradas: " + missingRoles);
        }
        return foundRoles;
    }
}