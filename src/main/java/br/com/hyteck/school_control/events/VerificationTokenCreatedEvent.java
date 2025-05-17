package br.com.hyteck.school_control.events;

import br.com.hyteck.school_control.models.auth.VerificationToken;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class VerificationTokenCreatedEvent extends ApplicationEvent {
    private final VerificationToken verificationToken;

    public VerificationTokenCreatedEvent(Object source, VerificationToken verificationToken) {
        super(source);
        this.verificationToken = verificationToken;
    }
}