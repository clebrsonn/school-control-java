package br.com.hyteck.school_control.usecases.user;

import br.com.hyteck.school_control.models.auth.User;
import br.com.hyteck.school_control.repositories.UserRepository;
import br.com.hyteck.school_control.web.dtos.user.PasswordChangeRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class ChangePasswordTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private ChangePassword changePassword;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = new BCryptPasswordEncoder();
        changePassword = new ChangePassword(userRepository, passwordEncoder);
    }

    @Test
    void testPasswordEncoding() {
        // Mock do usuário existente
        User user = new User();
        user.setUsername("testuser");
        user.setPassword(passwordEncoder.encode("oldPassword"));

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        // Requisição de troca de senha
        PasswordChangeRequest request = new PasswordChangeRequest("testuser", "oldPassword", "newPassword");

        // Executa o método
        changePassword.execute("testuser", request);

        // Verifica se a senha foi codificada
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertTrue(passwordEncoder.matches("newPassword", savedUser.getPassword()));
    }

    @Test
    void testInvalidCurrentPassword() {
        User user = new User();
        user.setUsername("testuser");
        user.setPassword(passwordEncoder.encode("oldPassword"));

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        PasswordChangeRequest request = new PasswordChangeRequest("testuser","wrongPassword", "newPassword");

        assertThrows(ResponseStatusException.class, () ->
                changePassword.execute("testuser", request)
        );
    }
}