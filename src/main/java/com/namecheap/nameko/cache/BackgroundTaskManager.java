package com.namecheap.nameko.cache;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.util.concurrency.AppExecutorUtil;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class BackgroundTaskManager {
    private static final int DEBOUNCE_DELAY_MS = 5000; // 5 seconds
    private ScheduledFuture<?> scheduledTask;

    public synchronized void scheduleTask(@NotNull Runnable task) {
        cancelScheduledTask();
        
        scheduledTask = AppExecutorUtil.getAppScheduledExecutorService().schedule(
            () -> ApplicationManager.getApplication().invokeLater(task),
            DEBOUNCE_DELAY_MS,
            TimeUnit.MILLISECONDS
        );
    }

    public synchronized void cancelScheduledTask() {
        if (scheduledTask != null && !scheduledTask.isDone()) {
            scheduledTask.cancel(false);
            scheduledTask = null;
        }
    }
} 