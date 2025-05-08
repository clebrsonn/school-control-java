package br.com.hyteck.school_control.usecases.user;


import br.com.hyteck.school_control.exceptions.ResourceNotFoundException;
import br.com.hyteck.school_control.models.auth.User;
import br.com.hyteck.school_control.models.auth.VerificationToken;
import br.com.hyteck.school_control.repositories.UserRepository;
import br.com.hyteck.school_control.repositories.VerificationTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VerifyAccount {

    private static final Logger logger = LoggerFactory.getLogger(VerifyAccount.class);
    private final VerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;

    public VerifyAccount(VerificationTokenRepository tokenRepository, UserRepository userRepository) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void execute(String tokenValue) {
        logger.debug("Tentando verificar conta com token: {}", tokenValue);

        // 1. Buscar o token
        VerificationToken verificationToken = tokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> {
                    logger.warn("Token de verificação não encontrado: {}", tokenValue);
                    return new ResourceNotFoundException("Token de verificação inválido.");
                });

        // 2. Verificar se expirou
        if (verificationToken.isExpired()) {
            logger.warn("Token de verificação expirado para o usuário {}: {}", verificationToken.getUser().getId(), tokenValue);
            // Opcional: Deletar token expirado aqui ou via job agendado
            // tokenRepository.delete(verificationToken);
            throw new RuntimeException("Token de verificação expirado. Por favor, solicite um novo.");
        }

        // 3. Ativar o usuário
        User user = verificationToken.getUser();
        if (user.isEnabled()) {
            logger.info("Usuário {} já estava ativo. Token: {}", user.getId(), tokenValue);
            // Pode deletar o token mesmo assim, pois foi usado
        } else {
            user.setEnabled(true);
            user.setAccountNonExpired(true);
            user.setCredentialsNonExpired(true);
            // user.setCredentialsNonExpired(true); // Garanta que as credenciais não estão expiradas
            userRepository.save(user);
            logger.info("Usuário {} ativado com sucesso via token {}", user.getId(), tokenValue);
        }


        // 4. (Recomendado) Deletar o token após o uso bem-sucedido
        tokenRepository.delete(verificationToken);
        logger.debug("Token de verificação {} deletado após uso.", tokenValue);
    }
}