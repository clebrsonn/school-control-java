package br.com.hyteck.school_control.usecases.notification;

import br.com.hyteck.school_control.repositories.NotificationRepository;
import lombok.extern.log4j.Log4j2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Log4j2
@Service
public class GetUnreadNotificationCountUseCase {
    private final NotificationRepository notificationRepository;

    public GetUnreadNotificationCountUseCase(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional(readOnly = true)
    public long execute(String userId) {
        log.debug("Buscando contagem de notificações não lidas para o usuário {}", userId);
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }
}