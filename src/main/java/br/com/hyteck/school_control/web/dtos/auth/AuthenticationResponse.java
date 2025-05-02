package br.com.hyteck.school_control.web.dtos.auth;

import br.com.hyteck.school_control.web.dtos.user.UserResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class AuthenticationResponse {
    private String token;
    private UserResponse user;
}
