package br.com.hyteck.school_control.usecases.notification;

import br.com.hyteck.school_control.models.Notification;
import br.com.hyteck.school_control.repositories.NotificationRepository;
import br.com.hyteck.school_control.web.dtos.NotificationResponse;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Log4j2
@Service
public class FindNotifications {

    private final NotificationRepository notificationRepository;

    public FindNotifications(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> execute(String userId, Pageable pageable) {
        log.debug("Buscando notificações para o usuário: {}, Paginação: {}", userId, pageable);
        Page<Notification> notificationPage = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return notificationPage.map(NotificationResponse::from);
    }}
