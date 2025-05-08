package br.com.hyteck.school_control.usecases.user;

import br.com.hyteck.school_control.exceptions.DuplicateResourceException;
import br.com.hyteck.school_control.exceptions.ResourceNotFoundException; // Ou BusinessRuleException
import br.com.hyteck.school_control.models.auth.Role;
import br.com.hyteck.school_control.models.auth.User;
import br.com.hyteck.school_control.models.auth.VerificationToken;
import br.com.hyteck.school_control.repositories.RoleRepository;
import br.com.hyteck.school_control.repositories.UserRepository;
import br.com.hyteck.school_control.repositories.VerificationTokenRepository;
import br.com.hyteck.school_control.usecases.notification.Notifications;
import br.com.hyteck.school_control.web.dtos.user.UserRequest;
import br.com.hyteck.school_control.web.dtos.user.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import lombok.extern.log4j.Log4j2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils; // Para verificar senha
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

    private final VerificationTokenRepository tokenRepository;
    private final Notifications notifications; // Injetar EmailService

    @Transactional
    public UserResponse execute(@Valid UserRequest requestDTO) {
        log.info("Iniciando criação de usuário: {}", requestDTO.username());

        if (!StringUtils.hasText(requestDTO.password())) {
            throw new IllegalArgumentException("Senha é obrigatória para criar usuário."); // Ou BusinessRuleException
        }

        if (userRepository.findByUsername(requestDTO.username()).isPresent()) {
            throw new DuplicateResourceException("Username já cadastrado: " + requestDTO.username());
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

        VerificationToken verificationToken = new VerificationToken(savedUser);
        tokenRepository.save(verificationToken);
        log.info("Token de verificação gerado para o usuário {}", savedUser.getUsername());

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                notifications.send(verificationToken);
            }
        });

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