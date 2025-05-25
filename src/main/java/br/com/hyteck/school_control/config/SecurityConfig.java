package br.com.hyteck.school_control.config;

import br.com.hyteck.school_control.config.jwt.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Configuração de segurança do aplicativo.
 *
 * @author Cleberson Chagas
 * @version 1.0
 * @since 2023-03-01
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * Provides a password encoder bean for hashing passwords.
     * Uses BCryptPasswordEncoder for strong, adaptive hashing.
     *
     * @return A {@link PasswordEncoder} instance.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Configures CORS (Cross-Origin Resource Sharing) for the application.
     * Allows all origins, methods, and headers for simplicity in development.
     * For production, specific origins should be configured.
     *
     * @return A {@link CorsConfigurationSource} instance.
     */
    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Allow requests from any origin.
        configuration.setAllowedOrigins(List.of("*")); // TODO: Restrict for production
        // Allow all common HTTP methods.
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        // Allow all headers.
        configuration.setAllowedHeaders(List.of("*"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // Apply this CORS configuration to all paths.
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * Configures the main security filter chain for the application.
     * This method defines how HTTP requests are secured, including CORS, CSRF,
     * session management, headers, request authorization, and custom filters.
     *
     * @param http The {@link HttpSecurity} object to configure.
     * @return The configured {@link SecurityFilterChain}.
     * @throws Exception If an error occurs during configuration.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Apply CORS configuration.
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // Disable CSRF protection as JWTs are used, making the application stateless against CSRF.
                .csrf(AbstractHttpConfigurer::disable)
                // Configure session management to be stateless, as JWTs handle session state.
                .sessionManagement((session) -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Configure various security headers.
                .headers(headers -> headers
                    // HTTP Strict Transport Security (HSTS) settings.
                    .httpStrictTransportSecurity(hsts -> hsts
                        .includeSubDomains(true) // Apply HSTS to all subdomains.
                        .preload(true) // Allow preloading HSTS policy by browsers.
                        .maxAgeInSeconds(31536000)) // HSTS policy valid for one year.
                    // Content Security Policy (CSP) settings.
                    .contentSecurityPolicy(csp -> csp
                        .policyDirectives("default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; img-src 'self' data:")) // Defines allowed sources for content.
                    // Frame Options to prevent clickjacking.
                    .frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin) // Allow framing only from the same origin.
                )
                // Configure authorization rules for HTTP requests.
                .authorizeHttpRequests(auth -> auth
                        // Permit all requests to Swagger UI and API docs.
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        // Permit POST requests to /users (e.g., for user registration).
                        .requestMatchers(HttpMethod.POST, "/users").permitAll()
                        // Permit requests to login and account verification endpoints.
                        .requestMatchers("/auth/login", "/auth/verify/**").permitAll()
                        // All other requests must be authenticated.
                        .anyRequest().authenticated()
                )
                // Add the custom JWT authentication filter before the standard UsernamePasswordAuthenticationFilter.
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                // Configure custom exception handling for authentication and authorization failures.
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        // Custom entry point for authentication failures (401 Unauthorized).
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setContentType("application/json");
                            response.setStatus(HttpStatus.UNAUTHORIZED.value());
                            response.getWriter().write(
                                    "{ \"error\": \"Unauthorized\", \"message\": \"" + authException.getMessage() + "\" }"
                            );
                        })
                        // Custom handler for access denied failures (403 Forbidden).
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setContentType("application/json");
                            response.setStatus(HttpStatus.FORBIDDEN.value());
                            response.getWriter().write(
                                    "{ \"error\": \"Forbidden\", \"message\": \"" + accessDeniedException.getMessage() + "\" }"
                            );
                        })
                );
        return http.build();
    }

    /**
     * Provides the {@link AuthenticationManager} bean.
     * This manager is responsible for authenticating users.
     *
     * @param authenticationConfiguration The {@link AuthenticationConfiguration} from Spring Security.
     * @return The configured {@link AuthenticationManager}.
     * @throws Exception If an error occurs while retrieving the authentication manager.
     */
    @Bean
    public AuthenticationManager authenticationManager(
            final AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

}
