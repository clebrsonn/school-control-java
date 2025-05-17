package br.com.hyteck.school_control.listeners;

import br.com.hyteck.school_control.events.VerificationTokenCreatedEvent;
import br.com.hyteck.school_control.models.auth.User;
import br.com.hyteck.school_control.models.auth.VerificationToken;
import br.com.hyteck.school_control.usecases.notification.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class VerificationEmailListener {

    private static final Logger logger = LoggerFactory.getLogger(VerificationEmailListener.class);
    private final EmailService asyncEmailSender;

    public VerificationEmailListener(EmailService asyncEmailSender) {
        this.asyncEmailSender = asyncEmailSender;
    }

    @Async
    @EventListener({VerificationTokenCreatedEvent.class})
    public void handleVerificationTokenCreated(VerificationTokenCreatedEvent event) {
        VerificationToken verificationToken = event.getVerificationToken();
        User user = verificationToken.getUser();

        if (user == null || user.getEmail() == null) {
            logger.warn("Não é possível enviar email de verificação. Usuário ou email nulo para o token: {}", verificationToken.getToken());
            return;
        }

        logger.info("Evento VerificationTokenCreatedEvent recebido para o token: {}. Tentando enviar email para {}",
                verificationToken.getToken(), user.getEmail());
        asyncEmailSender.send(verificationToken);

    }}