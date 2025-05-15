package br.com.hyteck.school_control.config.encoder;

import br.com.hyteck.school_control.models.auth.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

public class UserEntityListener {

    private static PasswordEncoder passwordEncoder;

    public static void setPasswordEncoder(PasswordEncoder encoder) {
        passwordEncoder = encoder;
    }

    @PrePersist
    @PreUpdate
    public void encodePassword(User user) {
        String password = user.getPassword();
        if (password != null && !password.startsWith("{bcrypt}")) { // ajuste conforme seu encoder
            user.setPassword(passwordEncoder.encode(password));
        }
    }
}