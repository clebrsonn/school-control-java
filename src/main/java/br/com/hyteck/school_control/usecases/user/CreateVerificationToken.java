package br.com.hyteck.school_control.usecases.user;

import br.com.hyteck.school_control.events.VerificationTokenCreatedEvent;
import br.com.hyteck.school_control.models.auth.User;
import br.com.hyteck.school_control.models.auth.VerificationToken;
import br.com.hyteck.school_control.repositories.VerificationTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Log4j2
public class CreateVerificationToken {
    private final VerificationTokenRepository tokenRepository;
    private final ApplicationEventPublisher eventPublisher;

    public void execute(User user){

        VerificationToken verificationToken = new VerificationToken(user);
        tokenRepository.save(verificationToken);
        log.info("Token de verificação gerado para o usuário {}", user.getUsername());

        eventPublisher.publishEvent(new VerificationTokenCreatedEvent(this, verificationToken));
        log.info("Evento VerificationTokenCreatedEvent publicado para o token: {}", verificationToken.getToken());

    }
}
