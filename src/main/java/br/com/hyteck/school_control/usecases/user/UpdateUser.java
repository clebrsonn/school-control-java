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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
// Não precisa do PasswordEncoder aqui, a menos que tenha um fluxo de troca de senha
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Validated
public class UpdateUser {

    private static final Logger logger = LoggerFactory.getLogger(UpdateUser.class);
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    // PasswordEncoder não é injetado aqui por padrão

    public UpdateUser(UserRepository userRepository, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    @Transactional
    public UserResponse execute(String id, @Valid UserRequest requestDTO) {
        logger.info("Iniciando atualização do usuário com ID: {}", id);

        // 1. Buscar usuário existente
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com ID: " + id));

        // 2. Verificar duplicidade SE username ou email mudaram
        checkDuplicates(requestDTO, existingUser);

        // 3. Validar e buscar Roles
        // 4. Atualizar dados da entidade
        existingUser.setUsername(requestDTO.username());
        existingUser.setEmail(requestDTO.email());
//        existingUser.setRoles(roles);

        // NÃO atualize a senha aqui por padrão.
        // Se uma senha foi fornecida no DTO, pode ser um erro ou exigir um fluxo diferente.
        if (StringUtils.hasText(requestDTO.password())) {
            logger.warn("Tentativa de atualizar senha do usuário {} através do endpoint de update geral. Senha não será alterada.", id);
            // Considere lançar uma exceção ou ter um endpoint/use case dedicado para troca de senha.
        }

        // Atualizar outros campos como 'enabled' se fizer parte deste fluxo

        // 5. Salvar
        User updatedUser = userRepository.save(existingUser);
        logger.info("Usuário atualizado com sucesso. ID: {}", updatedUser.getId());

        // 6. Retornar DTO
        return UserResponse.from(updatedUser);
    }

    private void checkDuplicates(UserRequest requestDTO, User existingUser) {
        // Verifica username apenas se foi alterado
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