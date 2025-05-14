// E:/IdeaProjects/school-control-java/src/main/java/br/com/hyteck/school_control/usecases/notification/MarkAllUserNotificationsAsReadUseCase.java
package br.com.hyteck.school_control.usecases.notification;

import br.com.hyteck.school_control.repositories.NotificationRepository;
import lombok.extern.log4j.Log4j2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Log4j2
@Service
public class MarkAllUserNotificationsAsReadUseCase {
    private final NotificationRepository notificationRepository;

    public MarkAllUserNotificationsAsReadUseCase(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public int execute(String userId) {
        log.debug("Tentando marcar todas as notificações como lidas para o usuário {}", userId);
        int updatedCount = notificationRepository.markAllAsReadForUser(userId, LocalDateTime.now());
        log.info("{} notificações marcadas como lidas para o usuário {}", updatedCount, userId);
        return updatedCount;
    }
}