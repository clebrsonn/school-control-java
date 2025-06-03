package br.com.hyteck.school_control.config.jwt;

import br.com.hyteck.school_control.config.jwt.provider.JWTProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Custom JWT authentication filter that intercepts HTTP requests to authenticate users.
 * This filter extends {@link OncePerRequestFilter} to ensure it's executed once per request.
 * It validates JWT tokens from the Authorization header and sets the authentication
 * in the {@link SecurityContextHolder} if the token is valid.
 */
@Component
//@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JWTProvider tokenProvider;
    private final UserDetailsService userDetailsService;

    /**
     * Constructs a new JwtAuthenticationFilter.
     *
     * @param tokenProvider      Service for JWT token generation and validation.
     * @param userDetailsService Service for loading user-specific data.
     */
    public JwtAuthenticationFilter(JWTProvider tokenProvider, UserDetailsService userDetailsService) {
        this.tokenProvider = tokenProvider;
        this.userDetailsService = userDetailsService;
    }

    /**
     * Processes an incoming HTTP request to authenticate the user based on a JWT token.
     * If a valid token is found in the "Authorization" header, it validates the token,
     * loads the user details, and sets the authentication in the security context.
     *
     * @param request     The {@link HttpServletRequest} object that contains the request the client made of the servlet.
     * @param response    The {@link HttpServletResponse} object that contains the response the servlet sends to the client.
     * @param filterChain The {@link FilterChain} for invoking the next filter or the resource at the end of the chain.
     * @throws ServletException If an input or output error is detected when the servlet handles the request.
     * @throws IOException      If an input or output error is detected when the servlet handles the request.
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        // Attempt to extract the JWT token from the request's Authorization header.
        String token = getJwtFromRequest(request);

        // Check if a token was found, if it's valid, and if no authentication is currently set in the SecurityContext.
        if (token != null && tokenProvider.validateToken(token) && SecurityContextHolder.getContext().getAuthentication() == null) {
            // Extract username from the valid token.
            String username = tokenProvider.getUsernameFromToken(token);

            UserDetails userDetails;
            try {
                // Load user details by username.
                userDetails = userDetailsService.loadUserByUsername(username);
            } catch (final UsernameNotFoundException userNotFoundEx) {
                // If user is not found, log the issue (implicitly done by UserDetailsService) and continue the filter chain without authentication.
                logger.warn("User not found for token: " + username, userNotFoundEx);
                filterChain.doFilter(request, response);
                return;
            }

            // Create an authentication token with the loaded user details and authorities.
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            // Set additional details for the authentication, such as IP address and browser information.
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            // Set the authentication in the SecurityContextHolder.
            SecurityContextHolder.getContext().setAuthentication(authentication);
            logger.debug("User '" + username + "' authenticated successfully.");
        }

        // Continue the filter chain, allowing the request to proceed to the next filter or the target resource.
        filterChain.doFilter(request, response);
    }

    /**
     * Extracts the JWT token from the "Authorization" header of the HTTP request.
     * The token is expected to be in the "Bearer <token>" format.
     *
     * @param request The {@link HttpServletRequest} from which to extract the token.
     * @return The JWT token string if found and correctly formatted, otherwise {@code null}.
     */
    private String getJwtFromRequest(HttpServletRequest request) {
        // Get the Authorization header from the request.
        String bearerToken = request.getHeader("Authorization");
        // Check if the header exists and starts with "Bearer ".
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            // Extract the token part (substring after "Bearer ").
            return bearerToken.substring(7);
        }
        // Return null if no valid Bearer token is found.
        return null;
    }
}
