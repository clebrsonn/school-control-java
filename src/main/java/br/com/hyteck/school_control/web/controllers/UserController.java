package br.com.hyteck.school_control.web.controllers;

import br.com.hyteck.school_control.usecases.user.*; // Importar use cases
import br.com.hyteck.school_control.web.dtos.user.UserRequest;
import br.com.hyteck.school_control.web.dtos.user.UserResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize; // Para segurança baseada em método (opcional)
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/users") // Endpoint base para usuários
//@PreAuthorize("hasRole('ADMIN')") // Pode proteger a classe inteira se só ADMIN mexe
public class UserController {

    private final br.com.hyteck.school_control.usecases.user.CreateUser createUser;
    private final FindUserById findUserById;
    private final FindUsername findUsername;
    private final FindUsers findAllUsers;
    private final UpdateUser updateUser;
    private final DeleteUser deleteUser;

    public UserController(CreateUser createUser, FindUserById findUserById, FindUsername findUsername, FindUsers findAllUsers, UpdateUser updateUser, DeleteUser deleteUser) {
        this.createUser = createUser;
        this.findUserById = findUserById;
        this.findUsername = findUsername;
        this.findAllUsers = findAllUsers;
        this.updateUser = updateUser;
        this.deleteUser = deleteUser;
    }

    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserRequest requestDTO) {
        UserResponse createdUser = createUser.execute(requestDTO);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdUser.id())
                .toUri();
        return ResponseEntity.created(location).body(createdUser);
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()") // Garante que só requisições com token válido cheguem aqui
    public ResponseEntity<UserResponse> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        // 1. Verifica se a autenticação existe (defensivo, o filtro já deve ter feito)
        if (userDetails == null) {
            // Normalmente o filtro JWT já barraria isso, retornando 401.
            // Se chegar aqui, pode ser um erro de configuração.
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // 2. Pega o username do Principal (que foi extraído do JWT pelo filtro)
        String username = userDetails.getUsername();

        return findUsername.execute(username)
                .map(ResponseEntity::ok)
                .orElseGet(() -> {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
                });

        /* Alternativa com @AuthenticationPrincipal (se o filtro cria UserDetails):
        @GetMapping("/me")
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<UserResponse> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
             if (userDetails == null) {
                 return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
             }
             String username = userDetails.getUsername();
             // ... resto da lógica igual ...
        }
        */
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')") // Apenas ADMIN pode ver por ID (ou ajuste conforme necessário)
    public ResponseEntity<UserResponse> getUserById(@PathVariable String id) {
        return findUserById.execute(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')") // Apenas ADMIN pode listar todos
    public ResponseEntity<Page<UserResponse>> getAllUsers(
            @PageableDefault(size = 10, sort = "username") Pageable pageable) {
        Page<UserResponse> userPage = findAllUsers.execute(pageable);
        return ResponseEntity.ok(userPage);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')") // Apenas ADMIN pode atualizar
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable String id,
            @Valid @RequestBody UserRequest requestDTO) {
        UserResponse updatedUser = updateUser.execute(id, requestDTO);
        return ResponseEntity.ok(updatedUser);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteUser(@PathVariable String id) {
        deleteUser.execute(id);
    }
}