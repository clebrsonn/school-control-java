package br.com.hyteck.school_control.web.controllers;

import br.com.hyteck.school_control.config.jwt.provider.JWTProvider;
import br.com.hyteck.school_control.usecases.user.ChangePassword;
import br.com.hyteck.school_control.usecases.user.VerifyAccount;
import br.com.hyteck.school_control.web.dtos.auth.AuthenticationRequest;
import br.com.hyteck.school_control.web.dtos.auth.AuthenticationResponse;
import br.com.hyteck.school_control.web.dtos.user.PasswordChangeRequest;
import br.com.hyteck.school_control.web.dtos.user.UserResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;


import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@WebMvcTest(AuthenticationController.class)
class AuthenticationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @MockitoBean
    private JWTProvider jwtTokenService;
    @MockitoBean
    private AuthenticationManager authenticationManager;
    @MockitoBean
    private UserDetailsService userDetailsService;
    @MockitoBean
    private VerifyAccount verifyAccountUseCase;
    @MockitoBean
    private ChangePassword changePasswordUseCase;

    @Autowired
    private ObjectMapper objectMapper;

    private AuthenticationRequest authRequest;
    private UserDetails userDetails;
    private final String MOCK_USER_USERNAME = "testUser";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity()) // Important for tests involving Spring Security context
                .build();

        authRequest = new AuthenticationRequest(MOCK_USER_USERNAME, "password");
        userDetails = new User(MOCK_USER_USERNAME, "password", Collections.emptyList());
    }

    @Test
    void authenticate_ShouldReturnTokenAndUser_WhenCredentialsAreValid() throws Exception {
        // Arrange
        UserResponse userResponse = new UserResponse("id1", MOCK_USER_USERNAME, "test@example.com", Set.of("ROLE_USER"), true, true, true, true, LocalDateTime.now(), LocalDateTime.now());
        AuthenticationResponse authResponse = new AuthenticationResponse("test-token", userResponse);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null); // Successful authentication
        when(userDetailsService.loadUserByUsername(MOCK_USER_USERNAME)).thenReturn(userDetails);
        when(jwtTokenService.generateToken(MOCK_USER_USERNAME)).thenReturn("test-token");

        // Act & Assert
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("test-token"))
                .andExpect(jsonPath("$.user.username").value(MOCK_USER_USERNAME));

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userDetailsService).loadUserByUsername(MOCK_USER_USERNAME);
        verify(jwtTokenService).generateToken(MOCK_USER_USERNAME);
    }

    @Test
    void authenticate_ShouldReturnUnauthorized_WhenCredentialsAreInvalid() throws Exception {
        // Arrange
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        // Act & Assert
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isUnauthorized()); // Assuming GlobalExceptionHandler handles this
                                                       // or Spring Security default behavior
         verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
         verifyNoInteractions(userDetailsService);
         verifyNoInteractions(jwtTokenService);
    }

    @Test
    void verifyAccount_ShouldReturnOk_WhenTokenIsValid() throws Exception {
        // Arrange
        String validToken = "valid-verification-token";
        doNothing().when(verifyAccountUseCase).execute(validToken);

        // Act & Assert
        mockMvc.perform(get("/auth/verify")
                        .param("token", validToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("Conta verificada com sucesso! Você já pode fazer login."));

        verify(verifyAccountUseCase).execute(validToken);
    }

    @Test
    @WithMockUser(username = MOCK_USER_USERNAME) // Simulate authenticated user
    void changePassword_ShouldReturnNoContent_WhenRequestIsValidAndUserIsAuthenticated() throws Exception {
        // Arrange
        PasswordChangeRequest passwordChangeRequest = new PasswordChangeRequest(MOCK_USER_USERNAME,"oldPassword", "newPassword");
        doNothing().when(changePasswordUseCase).execute(eq(MOCK_USER_USERNAME), any(PasswordChangeRequest.class));

        // Act & Assert
        mockMvc.perform(put("/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(passwordChangeRequest)))
                .andExpect(status().isNoContent());

        verify(changePasswordUseCase).execute(eq(MOCK_USER_USERNAME), any(PasswordChangeRequest.class));
    }
    
    @Test
    void changePassword_ShouldReturnUnauthorized_WhenUserIsNotAuthenticated() throws Exception {
        // Arrange
        PasswordChangeRequest passwordChangeRequest = new PasswordChangeRequest(MOCK_USER_USERNAME,"oldPassword", "newPassword");

        // Act & Assert
        mockMvc.perform(put("/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(passwordChangeRequest)))
                .andExpect(status().isUnauthorized()); // Controller throws ResponseStatusException for UNAUTHORIZED

        verifyNoInteractions(changePasswordUseCase);
    }
}
