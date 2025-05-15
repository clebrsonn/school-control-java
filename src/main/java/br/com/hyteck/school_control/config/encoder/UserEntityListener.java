package br.com.hyteck.school_control.config.encoder;

import br.com.hyteck.school_control.models.auth.User;
import lombok.Setter;
import org.springframework.security.crypto.password.PasswordEncoder;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

public class UserEntityListener {

    @Setter
    private static PasswordEncoder passwordEncoder;

    @PrePersist
    @PreUpdate
    public void encodePassword(User user) {
        String password = user.getPassword();
        if (password != null && !password.startsWith("{bcrypt}")) {
            user.setPassword(passwordEncoder.encode(password));
        }
    }
}