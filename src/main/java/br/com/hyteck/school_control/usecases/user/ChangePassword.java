package br.com.hyteck.school_control.usecases.user;

import br.com.hyteck.school_control.models.auth.User;
import br.com.hyteck.school_control.repositories.UserRepository;
import br.com.hyteck.school_control.web.dtos.user.PasswordChangeRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ChangePassword {
    private static final Logger logger = LoggerFactory.getLogger(ChangePassword.class);
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public void execute(String username, PasswordChangeRequest request){
        logger.info("Tentativa de troca de senha para o usuário: {}", username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado: " + username));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            logger.warn("Senha atual incorreta fornecida para o usuário: {}", username);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Senha atual incorreta.");
        }

        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A nova senha não pode ser igual à senha atual.");
        }
        // Adicione outras regras de complexidade se desejar

        user.setPassword(request.newPassword());
        user.setCredentialsNonExpired(false);
        userRepository.save(user);

        logger.info("Senha alterada com sucesso para o usuário: {}", username);
    }
}
