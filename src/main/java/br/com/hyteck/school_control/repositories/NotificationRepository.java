package br.com.hyteck.school_control.repositories;

import br.com.hyteck.school_control.models.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    Page<Notification> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    // Para GET /notifications/unread/count
    long countByUserIdAndReadFalse(String userId);

    // Para PUT /notifications/{id}/read
    Optional<Notification> findByIdAndUserId(String id, String userId);

    // Para POST /notifications/read-all
    @Modifying
    @Query("UPDATE Notification n SET n.read = true, n.readAt = :readAt WHERE n.user.id = :userId AND n.read = false")
    int markAllAsReadForUser(@Param("userId") String userId, @Param("readAt") LocalDateTime readAt);
}