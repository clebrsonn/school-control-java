package br.com.hyteck.school_control.usecases.notification;

import br.com.hyteck.school_control.models.Notification;
import br.com.hyteck.school_control.models.auth.User;
import br.com.hyteck.school_control.repositories.NotificationRepository;
import br.com.hyteck.school_control.repositories.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Log4j2
public class CreateNotification {
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public CreateNotification(NotificationRepository notificationRepository, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    /**
     * Cria e persiste uma nova notificação para um usuário.
     *
     * @param userId  O ID do usuário que receberá a notificação.
     * @param message O conteúdo da notificação.
     * @param link    Opcional: um link para onde o usuário deve ser direcionado.
     * @param type    Opcional: um tipo/categoria para a notificação.
     * @return A entidade Notification salva.
     * @throws EntityNotFoundException se o usuário com o ID fornecido não for encontrado.
     */
    @Transactional
    public Notification execute(String userId, String message, String link, String type) {
        User user = userRepository.findById(userId) // Assumindo que o ID do User é String
                .orElseThrow(() -> {
                    log.warn("Tentativa de criar notificação para usuário inexistente: {}", userId);
                    // Você pode usar uma exceção mais específica do seu projeto se tiver,
                    // como ResourceNotFoundException.
                    return new EntityNotFoundException("Usuário não encontrado com ID: " + userId + " para criar notificação.");
                });

        Notification notification = Notification.builder()
                .user(user)
                .message(message)
                .link(link)
                .type(type)
                .read(false) // Nova notificação sempre começa como não lida
                // createdAt será preenchido automaticamente pelo @CreationTimestamp
                .build();

        Notification savedNotification = notificationRepository.save(notification);
        log.info("Notificação criada para o usuário {}: ID {}", userId, savedNotification.getId());

        // 4. (Opcional) Disparar um evento para outros listeners
        // Por exemplo, se você tiver um sistema de WebSockets para notificações em tempo real,
        // você poderia publicar um evento aqui com a 'savedNotification'.
        // Ex: applicationEventPublisher.publishEvent(new NotificationCreatedEvent(this, savedNotification));

        return savedNotification;
    }

    /**
     * Sobrecarga do método execute sem 'link' e 'type' para simplificar chamadas comuns.
     */
    @Transactional
    public Notification execute(String userId, String message) {
        return execute(userId, message, null, null);
    }
}