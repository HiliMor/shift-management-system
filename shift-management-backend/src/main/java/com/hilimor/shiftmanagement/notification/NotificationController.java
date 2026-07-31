package com.hilimor.shiftmanagement.notification;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public List<NotificationResponse> listMyNotifications(Authentication authentication) {
        return notificationService.listMyNotifications(authentication.getName());
    }

    @GetMapping("/unread-count")
    public UnreadNotificationCountResponse countMyUnreadNotifications(Authentication authentication) {
        return notificationService.countMyUnreadNotifications(authentication.getName());
    }

    @PostMapping("/{notificationId}/read")
    public NotificationResponse markMyNotificationRead(
            Authentication authentication,
            @PathVariable Long notificationId
    ) {
        return notificationService.markMyNotificationRead(authentication.getName(), notificationId);
    }
}
