package br.com.hyteck.school_control.usecases.user;

import br.com.hyteck.school_control.repositories.UserRepository;
import br.com.hyteck.school_control.web.dtos.user.UserResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class FindUserById {

    private static final Logger logger = LoggerFactory.getLogger(FindUserById.class);
    private final UserRepository userRepository;

    public FindUserById(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Optional<UserResponse> execute(String id) {
        logger.debug("Buscando usuário com ID: {}", id);
        return userRepository.findById(id)
                .map(UserResponse::from);
    }
}