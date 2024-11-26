package com.namecheap.nameko;

import com.intellij.notification.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class PluginLogger {
    private static final String NOTIFICATION_GROUP_ID = "RPC Plugin Notifications";
    private static final NotificationGroup BALLOON_GROUP = NotificationGroupManager.getInstance()
        .getNotificationGroup("RPC Plugin Notifications");
        
    private static final Logger LOG = Logger.getInstance("#com.namecheap.nameko");
    
    public static void init() {
        LOG.info("========== RPC Plugin Initializing ==========");
    }
    
    public static void debug(String message) {
        LOG.debug(message);
    }
    
    public static void info(String message) {
        LOG.info(message);
    }
    
    public static void warn(String message) {
        LOG.warn(message);
    }
    
    public static void error(String message, Throwable e) {
        LOG.error(message, e);
    }
    
    public static void showNotification(@NotNull Project project, 
                                      @NotNull String title,
                                      @NotNull String content,
                                      @NotNull NotificationType type) {
        Notification notification = BALLOON_GROUP.createNotification(title, content, type);
        Notifications.Bus.notify(notification, project);
    }
    
    public static void logPerformance(String operation, long startTime) {
        long duration = System.currentTimeMillis() - startTime;
        LOG.info(String.format("Performance: %s took %dms", operation, duration));
    }
}