package br.com.hyteck.school_control.usecases.user;

import br.com.hyteck.school_control.exceptions.ResourceNotFoundException;
import br.com.hyteck.school_control.models.auth.User;
import br.com.hyteck.school_control.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
// Importar SecurityContextHolder se for impedir auto-deleção
// import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeleteUser {

    private static final Logger logger = LoggerFactory.getLogger(DeleteUser.class);
    private final UserRepository userRepository;

    public DeleteUser(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public void execute(String id) {
        logger.info("Iniciando exclusão do usuário com ID: {}", id);

        // 1. Verificar existência
        User userToDelete = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com ID: " + id));

        // 2. (Opcional) Regras de Negócio para Exclusão
        // Ex: Impedir que o usuário logado se delete
        /*
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        if (userToDelete.getUsername().equals(currentUsername)) {
            throw new BusinessRuleException("Você não pode excluir sua própria conta.");
        }
        */
        // Ex: Impedir exclusão de um usuário admin específico?
        /*
        if ("admin_principal".equals(userToDelete.getUsername())) { // Exemplo
             throw new BusinessRuleException("Este usuário administrador não pode ser excluído.");
        }
        */

        // 3. Deletar
        userRepository.delete(userToDelete);
        logger.info("Usuário excluído com sucesso. ID: {}", id);
    }
}