package br.com.hyteck.school_control.config.encoder;

// Em uma classe @Configuration
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Autowired;

@Configuration
public class UserEntityListenerConfig {
    @Autowired
    public void configureUserEntityListener(PasswordEncoder encoder) {
        UserEntityListener.setPasswordEncoder(encoder);
    }
}