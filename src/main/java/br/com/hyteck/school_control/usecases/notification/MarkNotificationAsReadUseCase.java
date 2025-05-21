// E:/IdeaProjects/school-control-java/src/main/java/br/com/hyteck/school_control/usecases/notification/MarkNotificationAsReadUseCase.java
package br.com.hyteck.school_control.usecases.notification;

import br.com.hyteck.school_control.models.Notification;
import br.com.hyteck.school_control.repositories.NotificationRepository;
import br.com.hyteck.school_control.web.dtos.NotificationResponse;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

/**
 * Service responsible for marking a notification as read for a specific user.
 * Updates the notification's read status and timestamp if not already read.
 */
@Log4j2
@Service
@AllArgsConstructor
public class MarkNotificationAsReadUseCase {
    private final NotificationRepository notificationRepository;

    /**
     * Marks a notification as read for a given user and notification ID.
     *
     * @param userId         the ID of the user
     * @param notificationId the ID of the notification
     * @return the updated NotificationResponse DTO
     * @throws ResponseStatusException if the notification is not found or does not belong to the user
     */
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