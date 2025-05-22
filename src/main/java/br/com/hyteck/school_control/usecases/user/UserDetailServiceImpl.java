package br.com.hyteck.school_control.usecases.user;

import br.com.hyteck.school_control.repositories.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Service implementation for loading user-specific data for Spring Security.
 * This class implements the {@link UserDetailsService} interface, which is used
 * by Spring Security to handle user authentication and authorization.
 */
@Service
public class UserDetailServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Constructs a new UserDetailServiceImpl.
     *
     * @param userRepository The repository for accessing user data.
     */
    public UserDetailServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Loads a user by their username from the {@link UserRepository}.
     * This method is called by Spring Security during the authentication process.
     *
     * @param username The username (login) of the user to load.
     * @return A {@link UserDetails} object representing the user, which includes their username,
     *         password (hashed), authorities (roles), and account status.
     * @throws UsernameNotFoundException If no user is found with the given username.
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Attempt to find the user by their username in the repository.
        // The User model itself should implement UserDetails.
        return userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    // If the user is not found, throw UsernameNotFoundException,
                    // which is a standard Spring Security exception.
                    String errorMessage = "User not found with username: " + username;
                    // It's good practice to log this event as well.
                    // logger.warn(errorMessage); // Assuming a logger is available
                    return new UsernameNotFoundException(errorMessage);
                });
    }
}
