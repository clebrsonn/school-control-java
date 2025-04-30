package br.com.hyteck.school_control.usecases.user;

import br.com.hyteck.school_control.exceptions.DuplicateResourceException;
import br.com.hyteck.school_control.exceptions.ResourceNotFoundException; // Ou BusinessRuleException
import br.com.hyteck.school_control.models.auth.Role;
import br.com.hyteck.school_control.models.auth.User;
import br.com.hyteck.school_control.models.auth.VerificationToken;
import br.com.hyteck.school_control.repositories.RoleRepository;
import br.com.hyteck.school_control.repositories.UserRepository;
import br.com.hyteck.school_control.repositories.VerificationTokenRepository;
import br.com.hyteck.school_control.web.dtos.user.UserRequest;
import br.com.hyteck.school_control.web.dtos.user.UserResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils; // Para verificar senha
import org.springframework.validation.annotation.Validated;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Validated
public class CreateUser {

    private static final Logger logger = LoggerFactory.getLogger(CreateUser.class);
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    private final VerificationTokenRepository tokenRepository;
    private final EmailService emailService; // Injetar EmailService

    public CreateUser(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder, VerificationTokenRepository tokenRepository, EmailService emailService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenRepository = tokenRepository;
        this.emailService = emailService;
    }

    @Transactional
    public UserResponse execute(@Valid UserRequest requestDTO) {
        logger.info("Iniciando criação de usuário: {}", requestDTO.username());

        // 1. Validar senha obrigatória na criação
        if (!StringUtils.hasText(requestDTO.password())) {
            throw new IllegalArgumentException("Senha é obrigatória para criar usuário."); // Ou BusinessRuleException
        }

        // 2. Verificar duplicidade
        if (userRepository.findByUsername(requestDTO.username()).isPresent()) {
            throw new DuplicateResourceException("Username já cadastrado: " + requestDTO.username());
        }
        // Adicionar verificação de email duplicado se necessário no repositório
        // if (userRepository.existsByEmail(requestDTO.email())) {
        //     throw new DuplicateResourceException("Email já cadastrado: " + requestDTO.email());
        // }

        // 3. Validar e buscar Roles
        Set<Role> roles = findAndValidateRoles(Set.of("ROLE_USER"));

        // 4. Mapear DTO para Entidade
        User userToSave = User.builder()
                .username(requestDTO.username())
                .password(passwordEncoder.encode(requestDTO.password())) // Codificar senha
                .email(requestDTO.email())
                .roles(roles)
                // Definir padrões de status (geralmente true na criação)
                .enabled(true)
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .build();

        // 5. Persistir
        User savedUser = userRepository.save(userToSave);
        logger.info("Usuário criado com sucesso. ID: {}", savedUser.getId());

        // 6. Mapear para Resposta

        VerificationToken verificationToken = new VerificationToken(savedUser);
        tokenRepository.save(verificationToken);
        logger.info("Token de verificação gerado para o usuário {}", savedUser.getUsername());

        // Enviar email APÓS o commit da transação principal
        String userEmail = savedUser.getEmail();
        String tokenValue = verificationToken.getToken();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                emailService.sendVerificationEmail(userEmail, tokenValue);
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