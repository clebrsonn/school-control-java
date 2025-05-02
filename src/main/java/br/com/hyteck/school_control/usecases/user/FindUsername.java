package br.com.hyteck.school_control.usecases.user;

import br.com.hyteck.school_control.repositories.UserRepository;
import br.com.hyteck.school_control.web.dtos.user.UserResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class FindUsername {

    private static final Logger logger = LoggerFactory.getLogger(FindUsername.class);
    private final UserRepository userRepository;

    public FindUsername(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Optional<UserResponse> execute(String username) {
        logger.debug("Buscando usuário com username: {}", username);
        return userRepository.findByUsername(username)
                .map(UserResponse::from);
    }
}