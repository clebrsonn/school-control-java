package br.com.hyteck.school_control.repositories;

import br.com.hyteck.school_control.models.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
}