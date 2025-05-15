package br.com.hyteck.school_control.config.encoder;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class UserEntityListenerConfig {
    @Autowired
    public void configureUserEntityListener(PasswordEncoder encoder) {
        UserEntityListener.setPasswordEncoder(encoder);
    }
}