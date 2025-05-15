package br.com.hyteck.school_control.usecases.user;

import br.com.hyteck.school_control.exceptions.DuplicateResourceException;
import br.com.hyteck.school_control.exceptions.ResourceNotFoundException;
import br.com.hyteck.school_control.models.auth.User;
import br.com.hyteck.school_control.repositories.UserRepository;
import br.com.hyteck.school_control.web.dtos.user.UserRequest;
import br.com.hyteck.school_control.web.dtos.user.UserResponse;
import jakarta.validation.Valid;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import java.util.Objects;

@Service
@Validated
@Log4j2
public class UpdateUser {

    private final UserRepository userRepository;
    // PasswordEncoder não é injetado aqui por padrão

    public UpdateUser(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public UserResponse execute(String id, @Valid UserRequest requestDTO) {
        log.info("Iniciando atualização do usuário com ID: {}", id);

        // 1. Buscar usuário existente
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com ID: " + id));

        // 2. Verificar duplicidade SE username ou email mudaram
        checkDuplicates(requestDTO, existingUser);

        existingUser.setUsername(requestDTO.username());
        existingUser.setEmail(requestDTO.email());
//        existingUser.setRoles(roles);

        if (StringUtils.hasText(requestDTO.password())) {
            log.warn("Tentativa de atualizar senha do usuário {} através do endpoint de update geral. Senha não será alterada.", id);
        }

        User updatedUser = userRepository.save(existingUser);
        log.info("Usuário atualizado com sucesso. ID: {}", updatedUser.getId());

        return UserResponse.from(updatedUser);
    }

    private void checkDuplicates(UserRequest requestDTO, User existingUser) {
        if (!Objects.equals(requestDTO.username(), existingUser.getUsername())) {
            userRepository.findByUsername(requestDTO.username()).ifPresent(duplicate -> {
                if (!Objects.equals(duplicate.getId(), existingUser.getId())) {
                    throw new DuplicateResourceException("Username já cadastrado para outro usuário: " + requestDTO.username());
                }
            });
        }
        // Adicionar verificação de email duplicado se necessário
        // if (!Objects.equals(requestDTO.email(), existingUser.getEmail())) {
        //     userRepository.findByEmail(requestDTO.email()).ifPresent(duplicate -> {
        //         if (!Objects.equals(duplicate.getId(), existingUser.getId())) {
        //             throw new DuplicateResourceException("Email já cadastrado para outro usuário: " + requestDTO.email());
        //         }
        //     });
        // }
    }

}