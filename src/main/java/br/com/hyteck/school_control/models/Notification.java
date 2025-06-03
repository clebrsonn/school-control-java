package br.com.hyteck.school_control.models;

import br.com.hyteck.school_control.models.auth.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Represents a notification sent to a user within the system.
 * Notifications can include messages, links to relevant parts of the application,
 * and tracking for when they were sent and read.
 * This entity extends {@link AbstractModel} to include common fields like ID and audit timestamps.
 * Lombok's annotations ({@link Getter}, {@link Setter}, {@link Builder}, {@link AllArgsConstructor}, {@link NoArgsConstructor})
 * are used to generate boilerplate code.
 */
@Entity
@Table(name = "notifications") // Specifies the database table name.
@Getter // Lombok: Generates getter methods for all fields.
@Setter // Lombok: Generates setter methods for all fields.
@Builder // Lombok: Provides the builder pattern for object creation.
@AllArgsConstructor // Lombok: Generates a constructor with all arguments.
@NoArgsConstructor // Lombok: Generates a no-argument constructor.
public class Notification extends AbstractModel{

    /**
     * The user to whom this notification is addressed.
     * This is a many-to-one relationship, meaning many notifications can belong to one user.
     * CascadeType.ALL is used here, which means operations (persist, merge, remove, etc.) on
     * a Notification entity might cascade to the associated User. This should be used with caution,
     * especially for REMOVE operations, to ensure it aligns with desired data integrity rules.
     * Consider if CascadeType.MERGE or CascadeType.PERSIST might be more appropriate depending on use case.
     */
    @ManyToOne(cascade = CascadeType.ALL) // Defines the relationship and cascading behavior.
    @JoinColumn(name = "user_id") // Specifies the foreign key column in the 'notifications' table.
    private User user;

    /**
     * The main content or message of the notification.
     */
    @Column(nullable = false, columnDefinition = "TEXT") // Message content should not be null and can be long.
    private String message;

    /**
     * An optional URL or link associated with the notification, directing the user to a relevant page or resource.
     * For example, a link to an invoice, an announcement, or a specific entity.
     */
    @Column
    private String link;

    /**
     * The type or category of the notification (e.g., "NEW_INVOICE", "SYSTEM_ALERT", "MESSAGE_RECEIVED").
     * This can be used for filtering or displaying notifications appropriately in the UI.
     */
    @Column(nullable = false) // Type should not be null.
    private String type;

    /**
     * The timestamp indicating when the notification was actually sent or made available to the user.
     * This might differ from {@code createdAt} if notifications are queued or scheduled.
     */
    @Column
    private LocalDateTime sendAt;

    /**
     * The timestamp indicating when the user marked the notification as read.
     * This field is nullable, as unread notifications will not have a read timestamp.
     */
    @Column
    private LocalDateTime readAt;

    /**
     * A boolean flag indicating whether the notification has been read by the user.
     * Defaults to {@code false} (unread).
     * The {@code nullable = false} constraint ensures this field always has a value.
     */
    @Column(name = "is_read", nullable = false)
    @Builder.Default // Ensures 'read' defaults to false when using the Lombok builder.
    private boolean read = false;
}
