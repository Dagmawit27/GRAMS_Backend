package com.ethiorental.backend.notification.controller;

import com.ethiorental.backend.notification.dto.response.NotificationResponse;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequestMapping("/api/notifications")
public class NotificationSseController {

    private final Map<String, List<SseEmitter>> emitters = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final ScheduledExecutorService heartbeatScheduler = Executors.newSingleThreadScheduledExecutor();

    @PostConstruct
    public void init() {
        // Send a keep-alive ping comment every 25 seconds to keep SSE streams alive through proxies & browsers
        heartbeatScheduler.scheduleAtFixedRate(() -> {
            emitters.forEach((userId, list) -> {
                for (SseEmitter emitter : list) {
                    try {
                        emitter.send(SseEmitter.event().comment("ping"));
                    } catch (Exception e) {
                        list.remove(emitter);
                        try {
                            emitter.complete();
                        } catch (Exception ignored) {}
                    }
                }
            });
        }, 25, 25, TimeUnit.SECONDS);
    }

    @PreDestroy
    public void destroy() {
        heartbeatScheduler.shutdown();
        executor.shutdown();
        emitters.values().forEach(list -> list.forEach(emitter -> {
            try {
                emitter.complete();
            } catch (Exception ignored) {}
        }));
        emitters.clear();
    }

    private String normalizeChannel(String channel) {
        if (channel == null) return "";
        return channel.trim().toLowerCase();
    }

    /**
     * Subscribe to SSE notifications for a specific user or channel
     */
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeToNotifications(@RequestParam String userId) {
        String normalizedId = normalizeChannel(userId);
        log.info("Client subscribing to SSE channel: {} (normalized: {})", userId, normalizedId);

        // Timeout of 30 minutes
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);

        List<SseEmitter> list = emitters.computeIfAbsent(normalizedId, k -> new CopyOnWriteArrayList<>());
        list.add(emitter);

        Runnable cleanup = () -> {
            List<SseEmitter> currentList = emitters.get(normalizedId);
            if (currentList != null) {
                currentList.remove(emitter);
                if (currentList.isEmpty()) {
                    emitters.remove(normalizedId);
                }
            }
        };

        emitter.onCompletion(() -> {
            log.info("SSE connection completed for channel: {} (normalized: {})", userId, normalizedId);
            cleanup.run();
        });

        emitter.onTimeout(() -> {
            log.info("SSE connection timed out for channel: {} (normalized: {})", userId, normalizedId);
            cleanup.run();
            try {
                emitter.complete();
            } catch (Exception ignored) {}
        });

        emitter.onError((ex) -> {
            log.warn("SSE connection error for channel {} (normalized: {}): {}", userId, normalizedId, ex.getMessage());
            cleanup.run();
            try {
                emitter.completeWithError(ex);
            } catch (Exception ignored) {}
        });

        // Send initial connection event
        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("Connected to notification stream for " + userId)
                    .id(String.valueOf(System.currentTimeMillis())));
            log.info("Initial connection event sent for channel {} (normalized: {})", userId, normalizedId);
        } catch (IOException e) {
            log.error("Error sending initial event to channel {} (normalized: {})", userId, normalizedId, e);
            cleanup.run();
            try {
                emitter.completeWithError(e);
            } catch (Exception ignored) {}
            return emitter;
        }

        return emitter;
    }

    /**
     * Send a notification to a specific user or channel
     */
    public void sendNotificationToUser(String userId, NotificationResponse notification) {
        String normalizedId = normalizeChannel(userId);
        List<SseEmitter> list = emitters.get(normalizedId);
        if (list != null && !list.isEmpty()) {
            executor.execute(() -> {
                for (SseEmitter emitter : list) {
                    try {
                        emitter.send(SseEmitter.event()
                                .name("notification")
                                .data(notification));
                        log.info("Sent notification to channel {} (raw: {}) via SSE", normalizedId, userId);
                    } catch (Exception e) {
                        log.warn("Error sending notification to emitter on channel {} (raw: {}): {}", normalizedId, userId, e.getMessage());
                        list.remove(emitter);
                        try {
                            emitter.completeWithError(e);
                        } catch (Exception ignored) {}
                    }
                }
            });
        } else {
            log.debug("No active SSE connections for channel: {} (normalized: {})", userId, normalizedId);
        }
    }

    /**
     * Send unread count update to a specific user or channel
     */
    public void sendUnreadCountUpdate(String userId, long unreadCount) {
        String normalizedId = normalizeChannel(userId);
        List<SseEmitter> list = emitters.get(normalizedId);
        if (list != null && !list.isEmpty()) {
            executor.execute(() -> {
                for (SseEmitter emitter : list) {
                    try {
                        emitter.send(SseEmitter.event()
                                .name("unreadCount")
                                .data(Map.of("count", unreadCount)));
                        log.info("Sent unread count update to channel {} (raw: {}) via SSE", normalizedId, userId);
                    } catch (Exception e) {
                        log.warn("Error sending unread count to emitter on channel {} (raw: {}): {}", normalizedId, userId, e.getMessage());
                        list.remove(emitter);
                        try {
                            emitter.completeWithError(e);
                        } catch (Exception ignored) {}
                    }
                }
            });
        }
    }
}
