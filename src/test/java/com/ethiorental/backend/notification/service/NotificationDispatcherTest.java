package com.ethiorental.backend.notification.service;

import com.ethiorental.backend.notification.config.NotificationAsyncConfig;
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
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class NotificationDispatcherTest {

    @Mock
    private NotificationListener notificationListener;

    @Mock
    private Executor notificationExecutor;

    @InjectMocks
    private NotificationDispatcher dispatcher;

    private AutoCloseable closeable;
    private NotificationEvent sampleEvent;

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
        doAnswer(invocation -> {
            Runnable task = invocation.getArgument(0);
            task.run();
            return null;
        }).when(notificationExecutor).execute(any());
        sampleEvent = new NotificationEvent(
                NotificationType.PROPERTY_SUBMITTED,
                "user-123",
                "PROPERTY",
                "prop-99",
                "Property submitted successfully",
                Set.of(NotificationChannel.IN_APP)
        );
    }

    @AfterEach
    void tearDown() throws Exception {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clear();
        }
        if (closeable != null) {
            closeable.close();
        }
    }

    @Test
    @DisplayName("When no transaction is active, dispatch calls listener immediately")
    void testDispatchNoActiveTransaction() {
        dispatcher.dispatch(sampleEvent);

        verify(notificationListener, times(1)).onEvent(sampleEvent);
    }

    @Test
    @DisplayName("When transaction is active and COMMITS, listener is invoked exactly once after commit")
    void testDispatchActiveTransactionCommitted() {
        TransactionSynchronizationManager.initSynchronization();

        dispatcher.dispatch(sampleEvent);

        // Listener should NOT be invoked before commit
        verify(notificationListener, never()).onEvent(any());

        // Simulate transaction commit
        List<TransactionSynchronization> synchronizations = TransactionSynchronizationManager.getSynchronizations();
        for (TransactionSynchronization sync : synchronizations) {
            sync.afterCommit();
        }

        verify(notificationListener, times(1)).onEvent(sampleEvent);
    }

    @Test
    @DisplayName("When transaction is active and ROLLED BACK, listener is NEVER invoked")
    void testDispatchActiveTransactionRolledBack() {
        TransactionSynchronizationManager.initSynchronization();

        dispatcher.dispatch(sampleEvent);

        // Listener should NOT be invoked before completion
        verify(notificationListener, never()).onEvent(any());

        // Simulate transaction rollback
        List<TransactionSynchronization> synchronizations = TransactionSynchronizationManager.getSynchronizations();
        for (TransactionSynchronization sync : synchronizations) {
            sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
        }

        // Listener must NOT have been called
        verify(notificationListener, never()).onEvent(any());
    }

    @Test
    @DisplayName("dispatch genuinely runs on a separate thread with configured notificationExecutor prefix")
    void testDispatchRunsAsyncOnConfiguredThreadPool() throws InterruptedException {
        NotificationAsyncConfig config = new NotificationAsyncConfig();
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) config.notificationExecutor();

        AtomicReference<String> threadName = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        NotificationListener asyncListener = event -> {
            threadName.set(Thread.currentThread().getName());
            latch.countDown();
        };

        NotificationDispatcher asyncDispatcher = new NotificationDispatcher(asyncListener, executor);

        executor.submit(() -> asyncDispatcher.dispatch(sampleEvent));

        boolean completed = latch.await(2, TimeUnit.SECONDS);
        assertTrue(completed, "Async dispatch did not complete in time");
        assertTrue(threadName.get().startsWith("notif-"), 
                "Thread name should start with configured prefix 'notif-', was: " + threadName.get());

        executor.shutdown();
    }
}
