package br.com.hyteck.school_control.web.dtos;

import br.com.hyteck.school_control.models.Notification; // Importar a entidade
import com.fasterxml.jackson.annotation.JsonFormat; // Para formatar data

import java.time.LocalDateTime;

/**
 * Represents the data transfer object (DTO) for a notification response.
 * This record is used to send notification details to clients.
 *
 * @param id        The unique identifier of the notification.
 * @param userId    The ID of the user to whom the notification belongs.
 * @param title     The title of the notification, typically derived from {@link Notification#getType()}.
 * @param message   The content of the notification message.
 * @param isRead    A boolean flag indicating whether the notification has been read.
 * @param createdAt The timestamp when the notification was created, formatted as "yyyy-MM-dd HH:mm:ss".
 */
public record NotificationResponse(
        String id,
        String userId,
        String title,    // Mapped from notification.getType()
        String message,
        Boolean isRead,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") // Optional formatting for the frontend
        LocalDateTime createdAt // Mapped from notification.getCreatedAt()
) {

    /**
     * Static factory method to create a {@link NotificationResponse} from a {@link Notification} entity.
     *
     * @param notification The {@link Notification} entity to convert.
     * @return A {@link NotificationResponse} populated with data from the entity, or {@code null} if the input is {@code null}.
     */
    public static NotificationResponse from(Notification notification) {
        // Return null if the source notification entity is null.
        if (notification == null) {
            return null;
        }
        // Create and return a new NotificationResponse.
        return new NotificationResponse(
                notification.getId(), // Map the ID.
                notification.getUser() != null ? notification.getUser().getId() : null, // Map userId, handling potential null User.
                notification.getType(), // Use the notification's type as the title.
                notification.getMessage(), // Map the message content.
                notification.isRead(), // Map the read status.
                notification.getCreatedAt() // Map the creation timestamp.
        );
    }
}