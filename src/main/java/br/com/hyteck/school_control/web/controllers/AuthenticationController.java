package br.com.hyteck.school_control.web.controllers;

import br.com.hyteck.school_control.config.jwt.provider.JWTProvider;
import br.com.hyteck.school_control.usecases.user.ChangePassword;
import br.com.hyteck.school_control.usecases.user.VerifyAccount;
import br.com.hyteck.school_control.web.dtos.auth.AuthenticationRequest;
import br.com.hyteck.school_control.web.dtos.auth.AuthenticationResponse;
import br.com.hyteck.school_control.web.dtos.user.PasswordChangeRequest;
import br.com.hyteck.school_control.web.dtos.user.UserResponse;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
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

/**
 * Controller responsible for handling user authentication, account verification,
 * and password management.
 */
@RestController
@RequestMapping("/auth")
@AllArgsConstructor
public class AuthenticationController {
    private final JWTProvider jwtTokenService;

    private final AuthenticationManager authenticationManager;

    private final UserDetailsService userDetailsService;

    private final VerifyAccount verifyAccountUseCase;

    private final ChangePassword changePasswordUseCase;

    /**
     * Authenticates a user and returns a JWT token along with user details.
     *
     * @param authenticationRequest The request body containing login credentials (username and password).
     * @return An {@link AuthenticationResponse} containing the JWT token and user information.
     * @throws BadCredentialsException If authentication fails due to incorrect credentials.
     */
    @PostMapping("/login")
    public AuthenticationResponse authenticate(@RequestBody @Valid final AuthenticationRequest authenticationRequest) {
            // Attempt to authenticate the user with the provided credentials.
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                    authenticationRequest.getLogin(), authenticationRequest.getPassword()));

        // Load user details after successful authentication.
        final UserDetails userDetails = userDetailsService.loadUserByUsername(authenticationRequest.getLogin());
        final AuthenticationResponse authenticationResponse = new AuthenticationResponse();
        // Generate JWT token.
        authenticationResponse.setToken(jwtTokenService.generateToken(userDetails.getUsername()));
        // Create a UserResponse object with relevant user details.
        // Note: Some fields like email are set to empty string as they might not be directly available from UserDetails.
        UserResponse userResponse = new UserResponse("", userDetails.getUsername(), "", userDetails.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet()),
                userDetails.isEnabled(), userDetails.isAccountNonLocked(),
                userDetails.isAccountNonExpired(), userDetails.isCredentialsNonExpired(), null, null);
        authenticationResponse.setUser(userResponse);
        return authenticationResponse;
    }

    /**
     * Verifies a user's account using a verification token.
     *
     * @param token The verification token sent to the user's email.
     * @return A ResponseEntity indicating the outcome of the verification process.
     */
    @GetMapping("/verify")
    public ResponseEntity<String> verifyAccount(@RequestParam("token") String token) {
        verifyAccountUseCase.execute(token);
        // TODO: Consider redirecting to a frontend page indicating successful verification.
        // Example: return ResponseEntity.status(HttpStatus.FOUND).location(URI.create("/login?verified=true")).build();

        // Return a success message.
        return ResponseEntity.ok("Conta verificada com sucesso! Você já pode fazer login.");

    }

    /**
     * Allows an authenticated user to change their password.
     *
     * @param request The request body containing the current and new passwords.
     * @return A ResponseEntity indicating success (204 No Content) or failure.
     * @throws ResponseStatusException If the user is not authenticated.
     */
    @PutMapping("/change-password")
    // No complex @PreAuthorize needed here, as SecurityContextHolder handles authentication checks.
    // The service layer (changePasswordUseCase) is responsible for verifying the current password.
    public ResponseEntity<?> changePassword(@Valid @RequestBody PasswordChangeRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // Ensure the user is authenticated before proceeding.
        if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal() instanceof String && authentication.getPrincipal().equals("anonymousUser")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário não autenticado.");
        }
        String username = ((UserDetails) authentication.getPrincipal()).getUsername();

        // Delegate password change logic to the use case.
        changePasswordUseCase.execute(
                username,
                request
        );
        // Return 204 No Content to indicate successful password change without a response body.
        return ResponseEntity.noContent().build();
    }
}
