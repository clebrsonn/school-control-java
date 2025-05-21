// E:/IdeaProjects/school-control-java/src/main/java/br/com/hyteck/school_control/web/controllers/NotificationController.java
package br.com.hyteck.school_control.web.controllers;

// Removidos imports não utilizados de billing

import br.com.hyteck.school_control.usecases.notification.FindNotifications;
import br.com.hyteck.school_control.usecases.notification.GetUnreadNotificationCountUseCase;
import br.com.hyteck.school_control.usecases.notification.MarkAllUserNotificationsAsReadUseCase;
import br.com.hyteck.school_control.usecases.notification.MarkNotificationAsReadUseCase;
import br.com.hyteck.school_control.web.dtos.NotificationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/notifications")
@Tag(name = "Notifications", description = "API para gerenciamento de notificações do usuário")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class NotificationController {

    private static final Logger logger = LoggerFactory.getLogger(NotificationController.class);

    private final FindNotifications findUserNotificationsUseCase;
    private final GetUnreadNotificationCountUseCase getUnreadNotificationCountUseCase;
    private final MarkNotificationAsReadUseCase markNotificationAsReadUseCase;
    private final MarkAllUserNotificationsAsReadUseCase markAllUserNotificationsAsReadUseCase;


    private String getAuthenticatedUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof UserDetails)) {
            logger.warn("Tentativa de acesso sem autenticação válida ou principal inválido.");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário não autenticado ou Principal inválido.");
        }
        // Assumindo que o 'username' do UserDetails é o ID do seu User.
        // Se for diferente, ajuste esta lógica.
        // Ex: Se o principal for a sua entidade User:
        if (authentication.getPrincipal() instanceof br.com.hyteck.school_control.models.auth.User authenticatedUser) {
            return authenticatedUser.getId();
        }
        return ((UserDetails) authentication.getPrincipal()).getUsername();
    }

    /**
     * Get all notifications for the current user
     *
     * @returns Page of notifications
     */
    @GetMapping
    @Operation(summary = "Listar notificações do usuário autenticado",
            description = "Retorna uma lista paginada das notificações do usuário.")
    public ResponseEntity<Page<NotificationResponse>> getNotifications(
            Authentication authentication,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        String userId = getAuthenticatedUserId(authentication);
        logger.info("Listando notificações para o usuário: {}", userId);
        Page<NotificationResponse> notifications = findUserNotificationsUseCase.execute(userId, pageable);
        return ResponseEntity.ok(notifications);
    }

    /**
     * Get unread notifications count for the current user
     *
     * @returns Number of unread notifications
     */
    @GetMapping("/unread/count")
    @Operation(summary = "Contar notificações não lidas",
            description = "Retorna o número de notificações não lidas para o usuário autenticado.")
    public ResponseEntity<Map<String, Long>> getUnreadNotificationsCount(Authentication authentication) {
        String userId = getAuthenticatedUserId(authentication);
        logger.info("Contando notificações não lidas para o usuário {}", userId);
        long count = getUnreadNotificationCountUseCase.execute(userId);
        return ResponseEntity.ok(Map.of("count", count)); // Retornando um JSON: {"count": N}
    }

    /**
     * Mark a notification as read
     *
     * @param id Notification ID
     * @returns Updated notification
     */
    @PutMapping("/{id}/read")
    @Operation(summary = "Marcar uma notificação como lida",
            description = "Marca uma notificação específica do usuário autenticado como lida.")
    public ResponseEntity<NotificationResponse> markNotificationAsRead(
            Authentication authentication,
            @PathVariable String id) {
        String userId = getAuthenticatedUserId(authentication);
        logger.info("Marcando notificação {} como lida para o usuário {}", id, userId);
        NotificationResponse notification = markNotificationAsReadUseCase.execute(userId, id);
        return ResponseEntity.ok(notification);
    }

    /**
     * Mark all notifications as read
     *
     * @returns Success message or count
     */
    @PostMapping("/read-all") // Usando POST conforme frontend, mas PUT também seria semanticamente aceitável
    @Operation(summary = "Marcar todas as notificações como lidas",
            description = "Marca todas as notificações não lidas do usuário autenticado como lidas.")
    public ResponseEntity<Map<String, String>> markAllNotificationsAsRead(Authentication authentication) {
        String userId = getAuthenticatedUserId(authentication);
        logger.info("Marcando todas as notificações como lidas para o usuário {}", userId);
        int count = markAllUserNotificationsAsReadUseCase.execute(userId);
        // O frontend espera uma string, mas retornar a contagem pode ser mais útil.
        // Vou retornar uma mensagem e a contagem.
        return ResponseEntity.ok(Map.of(
                "message", "Todas as notificações foram marcadas como lidas.",
                "updatedCount", String.valueOf(count)
        ));
    }
}