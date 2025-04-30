package br.com.hyteck.school_control.usecases.user;

import br.com.hyteck.school_control.exceptions.DuplicateResourceException;
import br.com.hyteck.school_control.models.auth.User;
import br.com.hyteck.school_control.repositories.UserRepository;
import br.com.hyteck.school_control.web.dtos.user.UserRequest;
import br.com.hyteck.school_control.web.dtos.user.UserResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CreateUserTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CreateUser createUser;

    @Test
    void testExecute() {
        UserRequest request = new UserRequest("testuser", "test@example.com", "password");
        when(userRepository.save(any(User.class))).thenReturn(new User());

        UserResponse response = createUser.execute(request);
        assertNotNull(response);
    }

    @Test
    void testDuplicateUser() {
        UserRequest request = new UserRequest("testuser", "test@example.com", "password");
        when(userRepository.findByUsername(request.username())).thenReturn(java.util.Optional.of(new User()));

        assertThrows(DuplicateResourceException.class, () -> createUser.execute(request));
    }
}
