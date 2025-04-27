package br.com.hyteck.school_control.usecases.user;

import br.com.hyteck.school_control.models.auth.User;
import br.com.hyteck.school_control.repositories.UserRepository;
import br.com.hyteck.school_control.web.dtos.user.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class FindUsers {
    private final UserRepository userRepository;

    public FindUsers(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Page<UserResponse> execute(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(UserResponse::from);
    }
}
