package br.com.hyteck.school_control.web.controllers;

import br.com.hyteck.school_control.usecases.user.*;
import br.com.hyteck.school_control.web.dtos.user.UserRequest;
import br.com.hyteck.school_control.web.dtos.user.UserResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @MockitoBean
    private CreateUser createUserUseCase;
    @MockitoBean
    private FindUserById findUserByIdUseCase;
    @MockitoBean
    private FindUsername findUsernameUseCase;
    @MockitoBean
    private FindUsers findAllUsersUseCase;
    @MockitoBean
    private UpdateUser updateUserUseCase;
    @MockitoBean
    private DeleteUser deleteUserUseCase;

    @Autowired
    private ObjectMapper objectMapper;

    private UserRequest userRequest;
    private UserResponse userResponse;
    private final String MOCK_ADMIN_USER = "adminUser";
    private final String MOCK_REGULAR_USER = "testUser";


    @BeforeEach
    void setUp() {
        // Apply Spring Security configuration to MockMvc
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity()) // Apply Spring Security
                .build();

        userRequest = new UserRequest("testuser", "password123", "testuser@example.com");
        userResponse = new UserResponse("userId1", "testuser", "testuser@example.com", Set.of("ROLE_USER"),
                true, true, true, true, LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    // No @WithMockUser needed as this endpoint should be open or handled by specific logic if not admin only
    void createUser_ShouldReturnCreated_WhenRequestIsValid() throws Exception {
        when(createUserUseCase.execute(any(UserRequest.class))).thenReturn(userResponse);

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequest)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(header().string("Location", "http://localhost/users/" + userResponse.id()))
                .andExpect(jsonPath("$.id").value(userResponse.id()))
                .andExpect(jsonPath("$.username").value(userResponse.username()));

        verify(createUserUseCase).execute(any(UserRequest.class));
    }

    @Test
    @WithMockUser(username = MOCK_REGULAR_USER, roles = {"USER"})
    void getCurrentUser_ShouldReturnUserResponse_WhenUserIsAuthenticated() throws Exception {
        when(findUsernameUseCase.execute(MOCK_REGULAR_USER)).thenReturn(Optional.of(userResponse));

        mockMvc.perform(get("/users/me")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(MOCK_REGULAR_USER));

        verify(findUsernameUseCase).execute(MOCK_REGULAR_USER);
    }
    
    @Test
    void getCurrentUser_ShouldReturnUnauthorized_WhenUserIsNotAuthenticated() throws Exception {
        mockMvc.perform(get("/users/me")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized()); // or 403 if filter chain processes it differently
    }


    @Test
    @WithMockUser(username = MOCK_ADMIN_USER, roles = {"ADMIN"})
    void getUserById_ShouldReturnUserResponse_WhenUserExistsAndAdminRole() throws Exception {
        when(findUserByIdUseCase.execute("userId1")).thenReturn(Optional.of(userResponse));

        mockMvc.perform(get("/users/{id}", "userId1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("userId1"));

        verify(findUserByIdUseCase).execute("userId1");
    }

    @Test
    @WithMockUser(username = MOCK_REGULAR_USER, roles = {"USER"})
    void getUserById_ShouldReturnForbidden_WhenUserRoleIsNotAdmin() throws Exception {
        mockMvc.perform(get("/users/{id}", "userId1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }


    @Test
    @WithMockUser(username = MOCK_ADMIN_USER, roles = {"ADMIN"})
    void getAllUsers_ShouldReturnPageOfUserResponses_WhenAdminRole() throws Exception {
        Pageable pageable = PageRequest.of(0, 10);
        List<UserResponse> userList = Collections.singletonList(userResponse);
        Page<UserResponse> userPage = new PageImpl<>(userList, pageable, userList.size());
        when(findAllUsersUseCase.execute(any(Pageable.class))).thenReturn(userPage);

        mockMvc.perform(get("/users")
                        .param("page", "0").param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(userResponse.id()));

        verify(findAllUsersUseCase).execute(any(Pageable.class));
    }

    @Test
    @WithMockUser(username = MOCK_ADMIN_USER, roles = {"ADMIN"})
    void updateUser_ShouldReturnUpdatedUserResponse_WhenAdminRoleAndRequestIsValid() throws Exception {
        UserResponse updatedResponse = new UserResponse("userId1", "updateduser", "updated@example.com", Set.of("ROLE_USER"),
                true, true, true, true, userResponse.createdAt(), LocalDateTime.now());
        when(updateUserUseCase.execute(anyString(), any(UserRequest.class))).thenReturn(updatedResponse);

        mockMvc.perform(put("/users/{id}", "userId1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("updateduser"));

        verify(updateUserUseCase).execute(anyString(), any(UserRequest.class));
    }

    @Test
    @WithMockUser(username = MOCK_ADMIN_USER, roles = {"ADMIN"})
    void deleteUser_ShouldReturnNoContent_WhenAdminRoleAndUserExists() throws Exception {
        doNothing().when(deleteUserUseCase).execute("userId1");

        mockMvc.perform(delete("/users/{id}", "userId1"))
                .andExpect(status().isNoContent());

        verify(deleteUserUseCase).execute("userId1");
    }
}
