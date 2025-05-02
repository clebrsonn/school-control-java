package br.com.hyteck.school_control.usecases.notification;

import br.com.hyteck.school_control.models.auth.VerificationToken;

public interface Notifications {

    void send(VerificationToken verificationToken);
}
