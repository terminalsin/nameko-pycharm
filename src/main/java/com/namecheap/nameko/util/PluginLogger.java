package com.namecheap.nameko.util;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.notification.*;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public final class PluginLogger {
    // Use a specific category for easy filtering
    private static final Logger LOG = Logger.getInstance("#com.namecheap.nameko.debug");

    private static final NotificationGroup NOTIFICATION_GROUP =
            NotificationGroupManager.getInstance().getNotificationGroup("RPC Plugin Notifications");

    public static void debug(@NotNull String message) {
        // Force debug messages to always show in Event Log
        LOG.warn("[DEBUG] " + message);

        // Also write to idea.log
        LOG.debug(message);
    }

    public static void info(@NotNull String message) {
        LOG.info(message);
    }

    public static void warn(@NotNull String message) {
        LOG.warn(message);
    }

    public static void error(@NotNull String message, Throwable e) {
        LOG.error(message, e);
    }

    public static void showNotification(@NotNull Project project,
                                        @NotNull String title,
                                        @NotNull String content,
                                        @NotNull NotificationType type) {
        ApplicationManager.getApplication().invokeLater(() -> {
            Notification notification = NOTIFICATION_GROUP
                    .createNotification(title, content, type);
            notification.notify(project);
        });
    }
}