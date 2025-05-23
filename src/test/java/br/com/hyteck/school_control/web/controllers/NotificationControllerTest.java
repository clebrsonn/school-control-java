package br.com.hyteck.school_control.web.controllers;

import br.com.hyteck.school_control.models.auth.User; // Assuming User model is needed for principal
import br.com.hyteck.school_control.usecases.notification.FindNotifications;
import br.com.hyteck.school_control.usecases.notification.GetUnreadNotificationCountUseCase;
import br.com.hyteck.school_control.usecases.notification.MarkAllUserNotificationsAsReadUseCase;
import br.com.hyteck.school_control.usecases.notification.MarkNotificationAsReadUseCase;
import br.com.hyteck.school_control.web.dtos.NotificationResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;


import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@WebMvcTest(NotificationController.class)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @MockBean
    private FindNotifications findUserNotificationsUseCase;
    @MockBean
    private GetUnreadNotificationCountUseCase getUnreadNotificationCountUseCase;
    @MockBean
    private MarkNotificationAsReadUseCase markNotificationAsReadUseCase;
    @MockBean
    private MarkAllUserNotificationsAsReadUseCase markAllUserNotificationsAsReadUseCase;

    @Autowired
    private ObjectMapper objectMapper;

    private NotificationResponse notificationResponse;
    private final String MOCK_USER_ID = "testUser"; // Corresponds to @WithMockUser(username="testUser")

    @BeforeEach
    void setUp() {
        // Ensure Spring Security context is considered by MockMvc
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .build();


        notificationResponse = new NotificationResponse(
                "notifId123",
                MOCK_USER_ID,
                "Test Notification",
                "This is a test notification.",
                false,
                LocalDateTime.now()
        );
    }

    @Test
    @WithMockUser(username = MOCK_USER_ID) // Simulate an authenticated user
    void getNotifications_ShouldReturnPageOfNotificationResponses() throws Exception {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        List<NotificationResponse> notificationList = Collections.singletonList(notificationResponse);
        Page<NotificationResponse> notificationPage = new PageImpl<>(notificationList, pageable, notificationList.size());

        when(findUserNotificationsUseCase.execute(eq(MOCK_USER_ID), any(Pageable.class)))
                .thenReturn(notificationPage);

        // Act & Assert
        mockMvc.perform(get("/notifications")
                        .param("page", String.valueOf(pageable.getPageNumber()))
                        .param("size", String.valueOf(pageable.getPageSize()))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(notificationResponse.id()))
                .andExpect(jsonPath("$.totalElements").value(notificationList.size()));

        verify(findUserNotificationsUseCase).execute(eq(MOCK_USER_ID), any(Pageable.class));
    }

    @Test
    @WithMockUser(username = MOCK_USER_ID)
    void getUnreadNotificationsCount_ShouldReturnCount() throws Exception {
        // Arrange
        long unreadCount = 5L;
        when(getUnreadNotificationCountUseCase.execute(MOCK_USER_ID)).thenReturn(unreadCount);

        // Act & Assert
        mockMvc.perform(get("/notifications/unread/count")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(unreadCount));

        verify(getUnreadNotificationCountUseCase).execute(MOCK_USER_ID);
    }

    @Test
    @WithMockUser(username = MOCK_USER_ID)
    void markNotificationAsRead_ShouldReturnUpdatedNotification() throws Exception {
        // Arrange
        String notificationId = "notifId123";
        NotificationResponse readNotificationResponse = new NotificationResponse(
                notificationId, MOCK_USER_ID, "Test Notification", "This is a test notification.", true, notificationResponse.createdAt()
        );
        when(markNotificationAsReadUseCase.execute(MOCK_USER_ID, notificationId)).thenReturn(readNotificationResponse);

        // Act & Assert
        mockMvc.perform(put("/notifications/{id}/read", notificationId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(notificationId))
                .andExpect(jsonPath("$.isRead").value(true));

        verify(markNotificationAsReadUseCase).execute(MOCK_USER_ID, notificationId);
    }

    @Test
    @WithMockUser(username = MOCK_USER_ID)
    void markAllNotificationsAsRead_ShouldReturnSuccessMessageAndCount() throws Exception {
        // Arrange
        int updatedCount = 3; // Assume 3 notifications were updated
        when(markAllUserNotificationsAsReadUseCase.execute(MOCK_USER_ID)).thenReturn(updatedCount);

        // Act & Assert
        mockMvc.perform(post("/notifications/read-all")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Todas as notificações foram marcadas como lidas."))
                .andExpect(jsonPath("$.updatedCount").value(String.valueOf(updatedCount)));

        verify(markAllUserNotificationsAsReadUseCase).execute(MOCK_USER_ID);
    }

    @Test
    void getNotifications_ShouldReturnUnauthorized_WhenNotAuthenticated() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/notifications")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized()); // Or .isForbidden() depending on exact filter chain if @PreAuthorize is not hit first
    }
}
