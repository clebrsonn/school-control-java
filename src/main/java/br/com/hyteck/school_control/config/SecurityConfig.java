package br.com.hyteck.school_control.config;

import br.com.hyteck.school_control.config.jwt.JwtAuthenticationFilter;
import br.com.hyteck.school_control.config.jwt.provider.JWTProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity // Habilita a configuração de segurança web do Spring Security
public class SecurityConfig {

    private final JWTProvider tokenProvider;
    private final UserDetailsService userDetailsService;

    public SecurityConfig(JWTProvider tokenProvider, UserDetailsService userDetailsService) {
        this.tokenProvider = tokenProvider;
        this.userDetailsService = userDetailsService;
    }


    public static final String[] ENDPOINTS_WITH_AUTHENTICATION_NOT_REQUIRED = {
            "/users/login", //url que usaremos para fazer login
            "/users" //url que usaremos para criar um usuário
    };

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // --- Bean para Configurar as Regras de Segurança HTTP ---
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Desabilitar CSRF (Cross-Site Request Forgery) - Comum para APIs REST stateless
                // Se sua aplicação usa sessões e formulários web, reavalie a necessidade de CSRF.
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement((session) -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Configurar autorização para as requisições HTTP
                .authorizeHttpRequests(auth -> auth
                        // Permitir acesso público a endpoints específicos (ex: documentação, login)
                        // .requestMatchers("/public/**", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/", "/authenticate").permitAll()
                        .anyRequest()
                )
                .addFilterBefore(jwtRequestFilter(tokenProvider, userDetailsService), UsernamePasswordAuthenticationFilter.class);

                // Configurar o método de autenticação
                // .httpBasic(Customizer.withDefaults()); // Habilita HTTP Basic Auth (popup no browser/cliente)

        return http.build();
    }

    public JwtAuthenticationFilter jwtRequestFilter(final JWTProvider jwtUserDetailsService,
                                                    final UserDetailsService userDetailsService) {
        return new JwtAuthenticationFilter(jwtUserDetailsService, userDetailsService);
    }

//
//    @Bean
//    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
//        return http.getSharedObject(AuthenticationManagerBuilder.class)
//                .userDetailsService(userDetailsService)
//                .passwordEncoder(passwordEncoder())
//                .and()
//                .build();
//    }

    @Bean
    public AuthenticationManager authenticationManager(
            final AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }


}