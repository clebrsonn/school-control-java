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

/**
 * Event listener responsible for handling {@link VerificationTokenCreatedEvent}.
 * When a verification token is created, this listener triggers an asynchronous email
 * sending process to the user with the verification link.
 */
@Component
public class VerificationEmailListener {

    private static final Logger logger = LoggerFactory.getLogger(VerificationEmailListener.class);
    private final EmailService asyncEmailSender;

    /**
     * Constructs a new VerificationEmailListener.
     *
     * @param asyncEmailSender The service responsible for sending emails asynchronously.
     */
    public VerificationEmailListener(EmailService asyncEmailSender) {
        this.asyncEmailSender = asyncEmailSender;
    }

    /**
     * Handles the {@link VerificationTokenCreatedEvent} by sending a verification email to the user.
     * This method is executed asynchronously to avoid blocking the main thread.
     *
     * @param event The {@link VerificationTokenCreatedEvent} containing the verification token and associated user.
     */
    @Async // Ensures this method runs in a separate thread.
    @EventListener({VerificationTokenCreatedEvent.class}) // Listens for VerificationTokenCreatedEvent.
    public void handleVerificationTokenCreated(VerificationTokenCreatedEvent event) {
        VerificationToken verificationToken = event.getVerificationToken();
        User user = verificationToken.getUser();

        // Validate that the user and their email are present.
        if (user == null || user.getEmail() == null) {
            logger.warn("Cannot send verification email. User or email is null for token: {}", verificationToken.getToken());
            return; // Exit if user or email is missing.
        }

        logger.info("VerificationTokenCreatedEvent received for token: {}. Attempting to send email to: {}",
                verificationToken.getToken(), user.getEmail());

        // Delegate the email sending task to the EmailService.
        // This service is expected to handle the actual construction and sending of the email.
        asyncEmailSender.send(verificationToken);
        logger.info("Verification email sending process initiated for user: {}", user.getEmail());
    }
}