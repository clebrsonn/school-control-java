package br.com.hyteck.school_control.web.controllers;

import br.com.hyteck.school_control.config.jwt.provider.JWTProvider;
import br.com.hyteck.school_control.usecases.user.ChangePassword;
import br.com.hyteck.school_control.usecases.user.VerifyAccount;
import br.com.hyteck.school_control.web.dtos.auth.AuthenticationRequest;
import br.com.hyteck.school_control.web.dtos.auth.AuthenticationResponse;
import br.com.hyteck.school_control.web.dtos.user.PasswordChangeRequest;
import br.com.hyteck.school_control.web.dtos.user.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.stream.Collectors;

@RestController
@RequestMapping("/auth")
public class AuthenticationController {
    private final JWTProvider jwtTokenService;

    private final AuthenticationManager authenticationManager;

    private final UserDetailsService userDetailsService;

    private final VerifyAccount verifyAccountUseCase;

    private final ChangePassword changePasswordUseCase;

    public AuthenticationController(JWTProvider jwtTokenService, AuthenticationManager authenticationManager, UserDetailsService userDetailsService, VerifyAccount verifyAccountUseCase, ChangePassword changePasswordUseCase) {
        this.jwtTokenService = jwtTokenService;
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.verifyAccountUseCase = verifyAccountUseCase;
        this.changePasswordUseCase = changePasswordUseCase;
    }

    @PostMapping("/login")
    public AuthenticationResponse authenticate(@RequestBody @Valid final AuthenticationRequest authenticationRequest) {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                    authenticationRequest.getLogin(), authenticationRequest.getPassword()));
        final UserDetails userDetails = userDetailsService.loadUserByUsername(authenticationRequest.getLogin());
        final AuthenticationResponse authenticationResponse = new AuthenticationResponse();
        authenticationResponse.setToken(jwtTokenService.generateToken(userDetails.getUsername()));
        UserResponse userResponse = new UserResponse("", userDetails.getUsername(), "", userDetails.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet()),
                userDetails.isEnabled(), userDetails.isAccountNonLocked(),
                userDetails.isAccountNonExpired(), userDetails.isCredentialsNonExpired(), null, null);
        authenticationResponse.setUser(userResponse);
        return authenticationResponse;
    }

    @GetMapping("/verify")
    public ResponseEntity<String> verifyAccount(@RequestParam("token") String token) {
        verifyAccountUseCase.execute(token);
        // Idealmente, redirecionar para uma página de sucesso no frontend
        // return ResponseEntity.status(HttpStatus.FOUND).location(URI.create("/login?verified=true")).build();

        return ResponseEntity.ok("Conta verificada com sucesso! Você já pode fazer login.");

    }

    @PutMapping("/change-password")
    // Não precisa de @PreAuthorize complexo aqui, apenas isAuthenticated() é suficiente,
    // pois a lógica interna verifica a senha atual.
    public ResponseEntity<?> changePassword(@Valid @RequestBody PasswordChangeRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário não autenticado.");
        }
        String username = ((UserDetails) authentication.getPrincipal()).getUsername();

        changePasswordUseCase.execute(
                username,
                request
        );
        return ResponseEntity.noContent().build(); // 204 No Content indica sucesso sem corpo
    }
}
