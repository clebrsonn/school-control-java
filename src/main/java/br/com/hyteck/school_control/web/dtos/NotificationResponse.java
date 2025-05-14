package br.com.hyteck.school_control.web.dtos;

import br.com.hyteck.school_control.models.Notification; // Importar a entidade
import com.fasterxml.jackson.annotation.JsonFormat; // Para formatar data

import java.time.LocalDateTime;

public record NotificationResponse(
        String id,
        String userId,
        String title,    // Mapeado de notification.getType()
        String message,
        Boolean isRead,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") // Formatação opcional para o frontend
        LocalDateTime createdAt // Mapeado de notification.getCreatedAt()
) {
    public static NotificationResponse from(Notification notification) {
        if (notification == null) {
            return null;
        }
        return new NotificationResponse(
                notification.getId(),
                notification.getUser() != null ? notification.getUser().getId() : null,
                notification.getType(), // Usando 'type' como 'title'
                notification.getMessage(),
                notification.isRead(),
                notification.getCreatedAt()
        );
    }
}