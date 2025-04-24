package br.com.hyteck.school_control.usecases.user;

import br.com.hyteck.school_control.repositories.UserRepository;
import br.com.hyteck.school_control.web.dtos.user.UserResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FindUsers {

    private static final Logger logger = LoggerFactory.getLogger(FindUsers.class);
    private final UserRepository userRepository;

    public FindUsers(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> execute(Pageable pageable) {
        logger.info("Buscando todos os usuários paginados: {}", pageable);
        return userRepository.findAll(pageable)
                .map(UserResponse::from);
    }
}
