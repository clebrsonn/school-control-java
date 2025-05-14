// E:/IdeaProjects/school-control-java/src/main/java/br/com/hyteck/school_control/usecases/notification/MarkNotificationAsReadUseCase.java
package br.com.hyteck.school_control.usecases.notification;

import br.com.hyteck.school_control.models.Notification;
import br.com.hyteck.school_control.repositories.NotificationRepository;
import br.com.hyteck.school_control.web.dtos.NotificationResponse; // Path correto
import lombok.extern.log4j.Log4j2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;


import java.time.LocalDateTime;

@Log4j2
@Service
public class MarkNotificationAsReadUseCase {
    private final NotificationRepository notificationRepository;

    public MarkNotificationAsReadUseCase(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public NotificationResponse execute(String userId, String notificationId) {
        log.debug("Tentando marcar notificação ID {} como lida para o usuário {}", notificationId, userId);
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> {
                    log.warn("Notificação ID {} não encontrada ou não pertence ao usuário {}", notificationId, userId);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Notificação não encontrada ou acesso negado.");
                });

        if (!notification.isRead()) {
            notification.setRead(true);
            notification.setReadAt(LocalDateTime.now());
            Notification updatedNotification = notificationRepository.save(notification);
            log.info("Notificação ID {} marcada como lida para o usuário {}", notificationId, userId);
            return NotificationResponse.from(updatedNotification);
        } else {
            log.info("Notificação ID {} já estava marcada como lida para o usuário {}", notificationId, userId);
            return NotificationResponse.from(notification);
        }
    }
}