package br.com.hyteck.school_control.events;


import br.com.hyteck.school_control.listeners.UserEntityListener;
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