package br.com.hyteck.school_control.web.controllers;

import br.com.hyteck.school_control.config.jwt.provider.JWTProvider;
import br.com.hyteck.school_control.web.dtos.auth.AuthenticationRequest;
import br.com.hyteck.school_control.web.dtos.auth.AuthenticationResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class AuthenticationController {
    private final JWTProvider jwtTokenService;

    private final AuthenticationManager authenticationManager;

    private final UserDetailsService userDetailsService;

    public AuthenticationController(JWTProvider jwtTokenService, AuthenticationManager authenticationManager, UserDetailsService userDetailsService) {
        this.jwtTokenService = jwtTokenService;
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
    }

    @PostMapping("/authenticate")
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
        return authenticationResponse;
    }
}
