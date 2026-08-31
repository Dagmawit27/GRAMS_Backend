package com.ethiorental.backend.notification.controller;

import com.ethiorental.backend.notification.dto.response.NotificationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@RestController
@RequestMapping("/api/notifications")
public class NotificationSseController {

    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newCachedThreadPool();

    /**
     * Subscribe to SSE notifications for a specific user
     */
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeToNotifications(@RequestParam String userId) {
        log.info("User {} subscribing to SSE notifications", userId);
        
        // Create emitter with timeout of 30 minutes
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);
        
        emitters.put(userId, emitter);
        
        // Send initial connection event
        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("Connected to notification stream"));
        } catch (IOException e) {
            log.error("Error sending initial event to user {}", userId, e);
        }
        
        // Handle emitter completion
        emitter.onCompletion(() -> {
            log.info("SSE connection completed for user {}", userId);
            emitters.remove(userId);
        });
        
        emitter.onTimeout(() -> {
            log.info("SSE connection timed out for user {}", userId);
            emitters.remove(userId);
        });
        
        emitter.onError((ex) -> {
            log.error("SSE connection error for user {}", userId, ex);
            emitters.remove(userId);
        });
        
        return emitter;
    }
    
    /**
     * Send a notification to a specific user
     * This is called by the notification listeners
     */
    public void sendNotificationToUser(String userId, NotificationResponse notification) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter != null) {
            executor.execute(() -> {
                try {
                    emitter.send(SseEmitter.event()
                            .name("notification")
                            .data(notification));
                    log.info("Sent notification to user {} via SSE", userId);
                } catch (IOException e) {
                    log.error("Error sending notification to user {} via SSE", userId, e);
                    emitters.remove(userId);
                    emitter.completeWithError(e);
                }
            });
        } else {
            log.warn("No active SSE connection for user: {}", userId);
        }
    }
    
    /**
     * Send unread count update to a specific user
     */
    public void sendUnreadCountUpdate(String userId, long unreadCount) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter != null) {
            executor.execute(() -> {
                try {
                    emitter.send(SseEmitter.event()
                            .name("unreadCount")
                            .data(Map.of("count", unreadCount)));
                    log.info("Sent unread count update to user {} via SSE", userId);
                } catch (IOException e) {
                    log.error("Error sending unread count to user {} via SSE", userId, e);
                    emitters.remove(userId);
                    emitter.completeWithError(e);
                }
            });
        }
    }
}
