package br.com.hyteck.school_control.web.controllers;

import br.com.hyteck.school_control.config.jwt.provider.JWTProvider;
import br.com.hyteck.school_control.usecases.user.VerifyAccount;
import br.com.hyteck.school_control.web.dtos.auth.AuthenticationRequest;
import br.com.hyteck.school_control.web.dtos.auth.AuthenticationResponse;
import br.com.hyteck.school_control.web.dtos.user.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/auth")
public class AuthenticationController {
    private final JWTProvider jwtTokenService;

    private final AuthenticationManager authenticationManager;

    private final UserDetailsService userDetailsService;

    private final VerifyAccount verifyAccountUseCase;

    public AuthenticationController(JWTProvider jwtTokenService, AuthenticationManager authenticationManager, UserDetailsService userDetailsService, VerifyAccount verifyAccountUseCase) {
        this.jwtTokenService = jwtTokenService;
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.verifyAccountUseCase = verifyAccountUseCase;
    }

    @PostMapping("/login")
    public AuthenticationResponse authenticate(@RequestBody @Valid final AuthenticationRequest authenticationRequest) {
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                    authenticationRequest.getLogin(), authenticationRequest.getPassword()));
        } catch (final BadCredentialsException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        final UserDetails userDetails = userDetailsService.loadUserByUsername(authenticationRequest.getLogin());
        final AuthenticationResponse authenticationResponse = new AuthenticationResponse();
        authenticationResponse.setToken(jwtTokenService.generateToken(userDetails.getUsername()));
        UserResponse userResponse = new UserResponse("", userDetails.getUsername(), "", null, true, true, true, true, null, null);
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
}
