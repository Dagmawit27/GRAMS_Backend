package com.ethiorental.backend.notification.service;

import com.ethiorental.backend.shared.notification.NotificationChannel;
import com.ethiorental.backend.shared.notification.NotificationEvent;
import com.ethiorental.backend.shared.notification.NotificationType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Set;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class NotificationEventPublisherImplTest {

    @Mock
    private NotificationDispatcher dispatcher;

    @InjectMocks
    private NotificationEventPublisherImpl publisher;

    private AutoCloseable closeable;

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (closeable != null) {
            closeable.close();
        }
    }

    @Test
    @DisplayName("publish() delegates event to NotificationDispatcher without blocking")
    void testPublishDelegatesToDispatcher() {
        NotificationEvent event = new NotificationEvent(
                NotificationType.PROPERTY_APPROVED,
                "user-456",
                "PROPERTY",
                "prop-100",
                "Property approved",
                Set.of(NotificationChannel.IN_APP, NotificationChannel.EMAIL)
        );

        publisher.publish(event);

        verify(dispatcher, times(1)).dispatch(event);
    }
}
